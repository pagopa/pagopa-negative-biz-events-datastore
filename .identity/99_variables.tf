locals {
  github = {
    org        = "pagopa"
    repository = "pagopa-negative-biz-events-datastore"
  }
  prefix = "pagopa"
  product = "${local.prefix}-${var.env_short}"
  project = "${local.prefix}-${var.env_short}-${local.location_short}-${local.domain}"
  runner = "${local.prefix}-${var.env_short}-${local.location_short}"

  app_name = "github-${local.github.org}-${local.github.repository}-${var.prefix}-${local.domain}-${var.env}-aks"

  domain = "bizevents"
  location_short  = "weu"

  pagopa_apim_name = "${local.product}-apim"
  pagopa_apim_rg   = "${local.product}-api-rg"

  aks_cluster = {
    name                = "${local.product}-${local.location_short}-${var.env}-aks"
    resource_group_name = "${local.product}-${local.location_short}-${var.env}-aks-rg"
  }

  container_app_environment = {
    name           = "${local.prefix}-${var.env_short}-${local.location_short}-github-runner-cae",
    resource_group = "${local.prefix}-${var.env_short}-${local.location_short}-github-runner-rg",
  }
}

variable "env" {
  type = string
}

variable "env_short" {
  type = string
}

variable "prefix" {
  type    = string
  default = "pagopa"
  validation {
    condition = (
    length(var.prefix) <= 6
    )
    error_message = "Max length is 6 chars."
  }
}

variable "github_repository_environment" {
  type = object({
    protected_branches     = bool
    custom_branch_policies = bool
    reviewers_teams        = list(string)
  })
  description = "GitHub Continuous Integration roles"
  default     = {
    protected_branches     = false
    custom_branch_policies = true
    reviewers_teams        = ["pagopa-tech"]
  }

}


variable "k8s_kube_config_path_prefix" {
  type    = string
  default = "~/.kube"
}
