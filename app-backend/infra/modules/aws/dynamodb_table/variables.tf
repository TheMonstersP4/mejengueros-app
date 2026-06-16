variable "table_name" {
  description = "DynamoDB table name."
  type        = string
}

variable "billing_mode" {
  description = "DynamoDB billing mode."
  type        = string
  default     = "PAY_PER_REQUEST"
}

variable "hash_key" {
  description = "DynamoDB table hash key."
  type        = string
}

variable "range_key" {
  description = "Optional DynamoDB table range key."
  type        = string
  default     = null
  nullable    = true
}

variable "attributes" {
  description = "DynamoDB table attributes used by keys and indexes."
  type = list(object({
    name = string
    type = string
  }))
}

variable "global_secondary_indexes" {
  description = "DynamoDB global secondary indexes."
  type = list(object({
    name            = string
    hash_key        = string
    range_key       = optional(string)
    projection_type = optional(string, "ALL")
  }))
  default = []
}

variable "ttl_attribute_name" {
  description = "Optional DynamoDB TTL attribute name."
  type        = string
  default     = null
  nullable    = true
}

variable "point_in_time_recovery_enabled" {
  description = "Enable point-in-time recovery."
  type        = bool
  default     = false
}
