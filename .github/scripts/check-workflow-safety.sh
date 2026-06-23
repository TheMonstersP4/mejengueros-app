#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd -- "$script_dir/../.." && pwd)"

export REPO_ROOT="$repo_root"

ruby <<'RUBY'
require 'json'
require 'yaml'

def fail!(invariant, message)
  warn "FAIL: #{invariant} - #{message}"
  exit 1
end

def load_yaml(path)
  YAML.load_file(path, aliases: true)
rescue StandardError => error
  fail!("yaml parse", "could not parse #{path}: #{error.class}: #{error.message}")
end

def load_json(path)
  JSON.parse(File.read(path))
rescue StandardError => error
  fail!("json parse", "could not parse #{path}: #{error.class}: #{error.message}")
end

def fetch_job(workflow, workflow_path, job_name)
  jobs = workflow['jobs']
  fail!(workflow_path, "expected top-level jobs mapping, found #{jobs.inspect}") unless jobs.is_a?(Hash)

  job = jobs[job_name]
  fail!(workflow_path, "expected job #{job_name.inspect}, found #{jobs.keys.sort.inspect}") unless job.is_a?(Hash)

  job
end

def check_timeout(workflow_path, job_name, job)
  timeout = job['timeout-minutes']
  fail!("#{workflow_path} job #{job_name}", "missing timeout-minutes, found #{timeout.inspect}") if timeout.nil?
end

def fetch_step(job, workflow_path, job_name, step_name)
  steps = job['steps']
  fail!("#{workflow_path} job #{job_name}", "expected steps array, found #{steps.inspect}") unless steps.is_a?(Array)

  step = steps.find { |candidate| candidate.is_a?(Hash) && candidate['name'].to_s == step_name }
  fail!("#{workflow_path} job #{job_name}", "missing step #{step_name.inspect}, found #{steps.map { |candidate| candidate.is_a?(Hash) ? candidate['name'] : candidate }.inspect}") unless step.is_a?(Hash)

  step
end

def check_trimmed_run(workflow_path, job_name, step_name, step, expected)
  actual = step['run']
  trimmed = actual.to_s.strip
  return if trimmed == expected

  fail!("#{workflow_path} job #{job_name} step #{step_name}", "expected run #{expected.inspect}, found #{actual.inspect}")
end

def normalize_needs(value)
  case value
  when nil
    []
  when String
    [value]
  when Array
    value.map(&:to_s)
  else
    [value.to_s]
  end
end

def check_needs_include(workflow_path, job_name, job, expected_need)
  normalized = normalize_needs(job['needs'])
  return if normalized.include?(expected_need)

  fail!("#{workflow_path} job #{job_name}", "expected needs to include #{expected_need.inspect}, found #{job['needs'].inspect}")
end

def check_script_defined(package_json_path, package_json, script_name)
  scripts = package_json['scripts']
  fail!(package_json_path, "expected scripts object, found #{scripts.inspect}") unless scripts.is_a?(Hash)

  value = scripts[script_name]
  fail!(package_json_path, "missing script #{script_name.inspect}, found #{scripts.keys.sort.inspect}") if value.nil?
end

repo_root = ENV.fetch('REPO_ROOT')
deploy_path = File.join(repo_root, '.github', 'workflows', 'deploy.yml')
backend_ci_path = File.join(repo_root, '.github', 'workflows', 'backend-ci.yml')
package_json_path = File.join(repo_root, 'app-backend', 'api', 'package.json')

deploy_workflow = load_yaml(deploy_path)
backend_ci_workflow = load_yaml(backend_ci_path)
package_json = load_json(package_json_path)

deploy_quality_job = fetch_job(deploy_workflow, deploy_path, 'quality-api')
check_timeout(deploy_path, 'quality-api', deploy_quality_job)

backend_quality_job = fetch_job(backend_ci_workflow, backend_ci_path, 'backend-quality-gate')
check_timeout(backend_ci_path, 'backend-quality-gate', backend_quality_job)

deploy_test_step = fetch_step(deploy_quality_job, deploy_path, 'quality-api', 'Test API')
check_trimmed_run(deploy_path, 'quality-api', 'Test API', deploy_test_step, 'npm run test:ci')

backend_test_step = fetch_step(backend_quality_job, backend_ci_path, 'backend-quality-gate', 'Unit tests')
check_trimmed_run(backend_ci_path, 'backend-quality-gate', 'Unit tests', backend_test_step, 'npm run test:ci')

check_script_defined(package_json_path, package_json, 'test:ci')
check_script_defined(package_json_path, package_json, 'test:ci:diagnose')

deploy_api_job = fetch_job(deploy_workflow, deploy_path, 'deploy-api')
check_needs_include(deploy_path, 'deploy-api', deploy_api_job, 'quality-api')

deploy_websocket_job = fetch_job(deploy_workflow, deploy_path, 'deploy-websocket-lambdas')
check_needs_include(deploy_path, 'deploy-websocket-lambdas', deploy_websocket_job, 'quality-api')

puts 'Workflow safety invariants passed.'
RUBY
