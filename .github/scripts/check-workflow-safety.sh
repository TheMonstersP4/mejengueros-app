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

def load_text(path)
  File.read(path)
rescue StandardError => error
  fail!("text read", "could not read #{path}: #{error.class}: #{error.message}")
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

def fetch_on(workflow, workflow_path)
  triggers = workflow['on'] || workflow[true]
  fail!(workflow_path, "expected top-level on mapping, found #{triggers.inspect}") unless triggers.is_a?(Hash)

  triggers
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

def check_equal(invariant, actual, expected)
  return if actual == expected

  fail!(invariant, "expected #{expected.inspect}, found #{actual.inspect}")
end

def check_include(invariant, haystack, needle)
  return if haystack.include?(needle)

  fail!(invariant, "expected to include #{needle.inspect}")
end

def check_match(invariant, text, pattern)
  return if text.match?(pattern)

  fail!(invariant, "expected pattern #{pattern.inspect}")
end

repo_root = ENV.fetch('REPO_ROOT')
deploy_path = File.join(repo_root, '.github', 'workflows', 'deploy.yml')
backend_ci_path = File.join(repo_root, '.github', 'workflows', 'backend-ci.yml')
project_in_progress_path = File.join(repo_root, '.github', 'workflows', 'project-in-progress.yml')
package_json_path = File.join(repo_root, 'app-backend', 'api', 'package.json')

deploy_workflow = load_yaml(deploy_path)
backend_ci_workflow = load_yaml(backend_ci_path)
project_in_progress_workflow = load_yaml(project_in_progress_path)
project_in_progress_text = load_text(project_in_progress_path)
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

project_on = fetch_on(project_in_progress_workflow, project_in_progress_path)
check_equal("#{project_in_progress_path} triggers", project_on.keys, ['pull_request_target'])

pull_request_target = project_on['pull_request_target']
fail!(project_in_progress_path, "expected pull_request_target mapping, found #{pull_request_target.inspect}") unless pull_request_target.is_a?(Hash)
check_equal(
  "#{project_in_progress_path} pull_request_target types",
  pull_request_target['types'],
  %w[opened edited reopened synchronize ready_for_review]
)

project_concurrency = project_in_progress_workflow['concurrency']
fail!(project_in_progress_path, "expected concurrency mapping, found #{project_concurrency.inspect}") unless project_concurrency.is_a?(Hash)
check_equal(
  "#{project_in_progress_path} concurrency group",
  project_concurrency['group'],
  'project-in-progress-pr-${{ github.event.pull_request.number }}'
)
check_equal("#{project_in_progress_path} concurrency cancel-in-progress", project_concurrency['cancel-in-progress'], true)

project_job = fetch_job(project_in_progress_workflow, project_in_progress_path, 'move-referenced-issues-to-in-progress')
check_timeout(project_in_progress_path, 'move-referenced-issues-to-in-progress', project_job)

project_env = project_job['env']
fail!(project_in_progress_path, "expected env mapping, found #{project_env.inspect}") unless project_env.is_a?(Hash)
check_equal("#{project_in_progress_path} PROJECT_ORG", project_env['PROJECT_ORG'], 'TheMonstersP4')
check_equal("#{project_in_progress_path} PROJECT_NUMBER", project_env['PROJECT_NUMBER'], '1')
check_equal("#{project_in_progress_path} PROJECT_ID", project_env['PROJECT_ID'], 'PVT_kwDOEUJAlM4BZWNS')
check_equal("#{project_in_progress_path} STATUS_FIELD_ID", project_env['STATUS_FIELD_ID'], 'PVTSSF_lADOEUJAlM4BZWNSzhUVdlM')
check_equal("#{project_in_progress_path} IN_PROGRESS_OPTION_ID", project_env['IN_PROGRESS_OPTION_ID'], '47fc9ee4')

project_steps = project_job['steps']
fail!(project_in_progress_path, "expected steps array, found #{project_steps.inspect}") unless project_steps.is_a?(Array)
if project_steps.any? { |step| step.is_a?(Hash) && step['uses'].to_s.start_with?('actions/checkout@') }
  fail!(project_in_progress_path, 'privileged workflow must not use actions/checkout')
end

project_step = fetch_step(
  project_job,
  project_in_progress_path,
  'move-referenced-issues-to-in-progress',
  'Move referenced issues to In progress'
)
check_equal(
  "#{project_in_progress_path} github-script version",
  project_step['uses'],
  'actions/github-script@v7'
)

project_script = project_step.dig('with', 'script').to_s
fail!(project_in_progress_path, 'expected embedded github-script body') if project_script.empty?

check_include("#{project_in_progress_path} action invariant comment", project_in_progress_text, 'Safety invariant: this privileged workflow intentionally pins github-script to the reviewed major tag.')
check_include("#{project_in_progress_path} trust guard REST lookup", project_script, 'github.rest.repos.getCollaboratorPermissionLevel')
check_include("#{project_in_progress_path} trusted permission write", project_script, "'write'")
check_include("#{project_in_progress_path} trusted permission maintain", project_script, "'maintain'")
check_include("#{project_in_progress_path} trusted permission admin", project_script, "'admin'")
check_match("#{project_in_progress_path} closing keywords", project_script, /closes\|fixes\|resolves/)
check_include("#{project_in_progress_path} in progress option", project_script, "currentStatus === 'In progress'")
check_include("#{project_in_progress_path} status mutation option id", project_script, 'optionId: inProgressOptionId')
check_include("#{project_in_progress_path} retry helper", project_script, 'async function withRetry')
check_include("#{project_in_progress_path} duplicate add handling", project_script, 'duplicate-item error')
check_include("#{project_in_progress_path} projectItems cap", project_script, 'projectItems(first: 100)')

trust_check_index = project_script.index('getCollaboratorPermissionLevel')
closing_keyword_index = project_script.index('const closePattern')
if trust_check_index.nil? || closing_keyword_index.nil? || trust_check_index > closing_keyword_index
  fail!(project_in_progress_path, 'trust guard must be defined before closing-keyword parsing and mutation logic')
end

puts 'Workflow safety invariants passed.'
RUBY
