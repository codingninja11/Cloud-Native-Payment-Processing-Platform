{{- define "base-service.name" -}}
{{- .Chart.Name -}}
{{- end -}}

{{- define "base-service.fullname" -}}
{{- printf "%s-%s" .Release.Name .Chart.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}

