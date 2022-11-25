package com.springernature.http4k.template

import com.springernature.grafanarama.Grafana
import com.springernature.grafanarama.model.*
import kotlin.system.exitProcess

fun main() {
    val grafanaUri = System.getenv("GF_URL")
    val grafanaApiKey = System.getenv("GF_API_KEY")

    if (grafanaUri.isNullOrBlank() || grafanaApiKey.isNullOrBlank()) {
        System.err.println("Error: you must define env vars GF_URL and GF_API_KEY")
        exitProcess(1)
    }

    val grafana = Grafana(grafanaUri, grafanaApiKey)

    val notificationChannel = grafana.upload(
        Notification(
            "an_alerts_channel_unique_id",
            name = "The Name Of The Slack Channel",
            type = "slack",
            default = false,
            sendReminder = false,
            settings = mapOf("url" to "https://hooks.slack.com/services/your/hook/uri")
        )
    )

    Template(grafana).createDashboard(
        "http4k-sn-template", // the name of your app (e.g. the service name in CF)
        "yourCfSpaceName", // the CF space where deployed
        "yourAppPrometheusName", // a unique name for your prometheus data source in Grafana
        "yourAppPrometheusUri", // the URI of your prometheus data source
        notificationChannel,
        true // true if you've configured the application in cf-snitch and want quota metrics
    )

}

class Template(private val grafana: Grafana) {

    fun createDashboard(
        appName: String,
        cfSpaceName: String,
        prometheusName: String,
        prometheusUri: String,
        notificationChannel: Notification,
        useCfSnitchMetrics: Boolean
    ) {
        val appDatasource = grafana.upload(Datasource(prometheusName, "prometheus", prometheusUri))

        grafana.upload(templateDashboard(appName, cfSpaceName, appDatasource, cfSnitchDataSource(useCfSnitchMetrics), notificationChannel))
    }

    private fun cfSnitchDataSource(useCfSnitchMetrics: Boolean): Datasource? = if (useCfSnitchMetrics) grafana.upload(
        Datasource(
            "coco-prometheus",
            "prometheus",
            "https://coco-prometheus.i-ris.io/prometheus"
        )
    ) else null


    private fun statusCode5xxAlert(
        appName: String,
        frequency: Int = 60,
        condition: Condition,
        notificationChannel: Notification
    ): Alert =
        Alert(
            "$appName | 5xx Errors ",
            frequency = "${frequency}s",
            message = "Service has exceeded 5xx error threshold",
            executionErrorState = ExecutionErrorState.KEEP_STATE,
            noDataState = NoDataState.KEEP_STATE
        )
            .withNotification(Notification(uid = notificationChannel.uid))
            .withCondition(condition)

    private fun aggregateResponsePanels(appName: String, cfSpaceName: String, appDatasource: Datasource, notificationChannel: Notification) =
        listOf(responsesByCodePanel(appName, cfSpaceName, appDatasource, notificationChannel), responseTimesPanel(appName, cfSpaceName, appDatasource))

    private fun responseTimesPanel(appName: String, cfSpaceName: String, appDatasource: Datasource): Panel {
        val alertMedianMetric = PrometheusMetric(
            "histogram_quantile(0.5, sum(rate(http_outgoing_requests_duration_seconds_bucket{iris_io_cf_app_space=\"$cfSpaceName\", iris_io_cf_app_name=~\"$appName(-qa)?\"}[2m])) by (le)) * 1000",
            hide = true
        )
        val alertUpper90Metric = PrometheusMetric(
            "histogram_quantile(0.9, sum(rate(http_outgoing_requests_duration_seconds_bucket{iris_io_cf_app_space=\"$cfSpaceName\", iris_io_cf_app_name=~\"$appName(-qa)?\"}[2m])) by (le)) * 1000",
            hide = true
        )
        return panelTemplate(
            appName,
            "response times",
            appDatasource,
            YAxisFormat.MILLISECONDS,
            grid = PanelGrid(leftMin = 0, leftMax = 5000)
        )
            .withMetric(
                PrometheusMetric(
                    "histogram_quantile(0.5, sum(rate(http_incoming_requests_duration_seconds_bucket{instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\", iris_io_cf_app_name=~\"$appName(-qa)?\"}[2m])) by (le)) * 1000",
                    legendFormat = "50th percentile"
                )
            )
            .withMetric(
                PrometheusMetric(
                    "histogram_quantile(0.9, sum(rate(http_incoming_requests_duration_seconds_bucket{instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\", iris_io_cf_app_name=~\"$appName(-qa)?\"}[2m])) by (le)) * 1000",
                    legendFormat = "90th percentile"
                )
            )
            .withMetric(
                PrometheusMetric(
                    "histogram_quantile(0.999, sum(rate(http_incoming_requests_duration_seconds_bucket{instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\", iris_io_cf_app_name=~\"$appName(-qa)?\"}[2m])) by (le)) * 1000",
                    legendFormat = "99.9th percentile"
                )
            )
            .withMetric(alertMedianMetric)
            .withMetric(alertUpper90Metric)
    }

    private fun responsesByCodePanel(
        appName: String,
        cfSpaceName: String,
        appDatasource: Datasource,
        notificationChannel: Notification
    ): Panel {
        val responsesByCodeColours = mapOf(
            "2xx" to "#70DBED",
            "3xx" to "#F2C96D",
            "4xx" to "#CCA300",
            "5xx" to "#FF0000"
        )

        val alert5xxMetric = PrometheusMetric(
            "sum (rate(http_incoming_requests_total{iris_io_cf_app_space=\"$cfSpaceName\", iris_io_cf_app_name=~\"$appName(-qa)?\", http_status=~\"5.*\"}[2m]))",
            hide = true
        )
        return GenericPanel(
            "$appName - responses by code",
            yaxisFormats = listOf(YAxisFormat.SHORT, YAxisFormat.SHORT),
            span = CustomSpan(6),
            fill = 10,
            aliasColours = responsesByCodeColours,
            stack = StackStyle.STACKED,
            datasource = appDatasource.name,
            grid = PanelGrid(leftMin = 0)
        )
            .withMetric(
                PrometheusMetric(
                    "sum (rate(http_incoming_requests_total{instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\", iris_io_cf_app_name=~\"$appName(-qa)?\", http_status=~\"2.*\"}[2m]))",
                    legendFormat = "2xx"
                )
            )
            .withMetric(
                PrometheusMetric(
                    "sum (rate(http_incoming_requests_total{instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\", iris_io_cf_app_name=~\"$appName(-qa)?\", http_status=~\"3.*\"}[2m]))",
                    legendFormat = "3xx"
                )
            )
            .withMetric(
                PrometheusMetric(
                    "sum (rate(http_incoming_requests_total{instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\", iris_io_cf_app_name=~\"$appName(-qa)?\", http_status=~\"4.*\"}[2m]))",
                    legendFormat = "4xx"
                )
            )
            .withMetric(
                PrometheusMetric(
                    "sum (rate(http_incoming_requests_total{instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\", iris_io_cf_app_name=~\"$appName(-qa)?\", http_status=~\"5.*\"}[2m]))",
                    legendFormat = "5xx"
                )
            )
            .withMetric(alert5xxMetric)
            .withAlert(
                statusCode5xxAlert(
                    appName,
                    condition = Condition(
                        Query(
                            QueryParameters(alert5xxMetric),
                            datasourceId = appDatasource.id ?: error("No ID set on datasource")
                        ),
                        evaluator = Evaluator(EvaluatorType.GREATER_THAN, 1.0),
                        reducer = Reducer(ReducerType.AVERAGE)
                    ),
                    notificationChannel = notificationChannel
                )
            )
    }

    private fun templateDashboard(
        appName: String,
        cfSpaceName: String,
        appDatasource: Datasource,
        cfSnitchDatasource: Datasource?,
        notificationChannel: Notification
    ): Dashboard = Dashboard(appName)
        .withVariable(
            QueryVariable(
                "p_environment",
                "Cloudfoundry Space",
                appDatasource.name,
                "label_values(process_uptime_seconds{iris_io_cf_app_name=~\"$appName.*\"},iris_io_cf_app_space)",
                sort = VariableSort.ALPHA_ASC
            )
        )
        .withVariable(
            QueryVariable(
                "p_instance",
                "Instance",
                appDatasource.name,
                "label_values(process_uptime_seconds{iris_io_cf_app_space=~\"\$p_environment\",iris_io_cf_app_name=~\"$appName(-qa)?\"},instance)",
                includeAll = true,
                allValue = ".*",
                sort = VariableSort.ALPHA_ASC,
                refresh = VariableRefresh.ON_TIME_RANGE_CHANGE
            )
        )
        .withRow(aggregateAppRow(appName, cfSpaceName, appDatasource, cfSnitchDatasource, notificationChannel))
        .withRow(instanceRow(appName, appDatasource))
        .withRow(jvmRow(appName, appDatasource))

    private fun aggregateCloudFoundryPanels(appName: String, cfSpaceName: String, cfSnitchDatasource: Datasource?) =
        cfSnitchDatasource?.let { datasource ->
            listOf(
                GenericPanel(
                    "$appName - live crashes",
                    yaxisFormats = listOf(YAxisFormat.SHORT, YAxisFormat.SHORT),
                    span = CustomSpan(6),
                    fill = 4,
                    stack = StackStyle.UNSTACKED,
                    lines = false,
                    bars = true,
                    datasource = datasource.name,
                    grid = PanelGrid(leftMin = 0)
                ).withMetric(
                    PrometheusMetric(
                        "rate(cf_app_crash_total{app_name=\"$appName\",space_name=\"$cfSpaceName\"}[2m])",
                        legendFormat = "crashes"
                    )
                ),
                GenericPanel(
                    "$appName - live container resources",
                    yaxisFormats = listOf(YAxisFormat.SHORT, YAxisFormat.SHORT),
                    span = CustomSpan(6),
                    fill = 4,
                    stack = StackStyle.UNSTACKED,
                    lines = true,
                    bars = false,
                    datasource = datasource.name,
                    grid = PanelGrid(leftMin = 0)
                )
                    .withMetric(
                        PrometheusMetric(
                            "cf_app_cpu_quota_percent{app_name=\"$appName\",space_name=\"$cfSpaceName\"}",
                            legendFormat = "cpu quota usage"
                        )
                    )
                    .withMetric(
                        PrometheusMetric(
                            "cf_app_mem_percent{app_name=\"$appName\",space_name=\"$cfSpaceName\"}",
                            legendFormat = "container memory usage"
                        )
                    )
            )
        } ?: listOf()

    private fun circuitBreakerPanels(appName: String, datasource: Datasource) = listOf(
        GenericPanel(
            "$appName - circuit-breaker failures",
            yaxisFormats = listOf(YAxisFormat.SHORT, YAxisFormat.SHORT),
            span = CustomSpan(6),
            fill = 4,
            stack = StackStyle.STACKED,
            datasource = datasource.name,
            grid = PanelGrid(leftMin = 0)
        ).withMetric(
            PrometheusMetric(
                "sum by (circuitbreaker_name) (rate(circuitbreaker_failure_total{iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"}[2m]) > 0)",
                legendFormat = "{{circuitbreaker_name}} fail"
            )
        )
            .withMetric(
                PrometheusMetric(
                    "sum by (circuitbreaker_name) (rate(circuitbreaker_shortcircuit_total{iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"}[2m]) > 0)",
                    legendFormat = "{{circuitbreaker_name}} short-circuit"
                )
            )
    )

    private fun httpTimingPanels(appName: String, appDatasource: Datasource) = listOf(
        panelTemplate(appName, "90%ile outgoing timings", appDatasource, YAxisFormat.MILLISECONDS).withMetric(
            PrometheusMetric(
                "histogram_quantile(0.9, sum(rate(http_outgoing_requests_duration_seconds_bucket{iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"}[2m])) by (le, request_uri)) * 1000",
                legendFormat = "{{request_uri}}"
            )
        ),
        panelTemplate(appName, "90%ile response times by path", appDatasource, YAxisFormat.MILLISECONDS).withMetric(
            PrometheusMetric(
                "histogram_quantile(0.9, sum(rate(http_incoming_requests_duration_seconds_bucket{iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"}[2m])) by (le, request_path)) * 1000",
                legendFormat = "{{request_path}}"
            )
        )
    )

    private fun httpClientServerPanels(appName: String, appDatasource: Datasource) = listOf(
        panelTemplate(appName, "http client", appDatasource, fill = 1).withMetric(
            PrometheusMetric(
                "max(httpclient_connectionpool_active_count{iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                legendFormat = "active"
            )
        )
            .withMetric(
                PrometheusMetric(
                    "max(httpclient_connectionpool_queue_count{iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                    legendFormat = "queued"
                )
            )
            .withMetric(
                PrometheusMetric(
                    "max(httpclient_connectionpool_available_count{iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                    legendFormat = "available"
                )
            )
            .withMetric(
                PrometheusMetric(
                    "max(httpclient_connectionpool_max_count{iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                    legendFormat = "max"
                )
            ),
        panelTemplate(appName, "jetty", appDatasource, fill = 1).withMetric(
            PrometheusMetric(
                "max(appserver_requestqueue_count{iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                legendFormat = "queue"
            )
        )
            .withMetric(
                PrometheusMetric(
                    "max(appserver_workerpool_active_count{iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                    legendFormat = "active"
                )
            )
            .withMetric(
                PrometheusMetric(
                    "max(appserver_workerpool_max_count{iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                    legendFormat = "max"
                )
            )
    )

    private fun aggregateAppRow(appName: String, cfSpaceName: String, appDatasource: Datasource, cfSnitchDatasource: Datasource?, notificationChannel: Notification) =
        Row(title = "Application Metrics", showTitle = true, collapse = false)
            .withPanels(aggregateResponsePanels(appName, cfSpaceName, appDatasource, notificationChannel))
            .withPanels(aggregateCloudFoundryPanels(appName, cfSpaceName, cfSnitchDatasource))

    private fun instanceRow(appName: String, appDatasource: Datasource) =
        Row(title = "Instance Metrics", showTitle = true, collapse = false)
            .withPanels(circuitBreakerPanels(appName, appDatasource))
            .withPanels(httpTimingPanels(appName, appDatasource))
            .withPanels(httpClientServerPanels(appName, appDatasource))

    private fun jvmRow(appName: String, appDatasource: Datasource) =
        Row(title = "JVM Metrics", showTitle = true, collapse = true)
            .withPanel(
                jvmPanelTemplate(appName, "threading", appDatasource)
                    .withMetric(
                        PrometheusMetric(
                            "max(jvm_threads_current{iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                            legendFormat = "current"
                        )
                    )
                    .withMetric(
                        PrometheusMetric(
                            "max(jvm_threads_peak{iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                            legendFormat = "peak"
                        )
                    )
            )
            .withPanel(
                jvmPanelTemplate(appName, "uptime", appDatasource, YAxisFormat.SECONDS)
                    .withMetric(
                        PrometheusMetric(
                            "max(process_uptime_seconds{iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                            legendFormat = "uptime"
                        )
                    )
            )
            .withPanel(
                jvmPanelTemplate(appName, "cpu", appDatasource, YAxisFormat.PERCENT)
                    .withMetric(
                        PrometheusMetric(
                            "max(system_cpu_usage_percent{iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                            legendFormat = "system"
                        )
                    )
                    .withMetric(
                        PrometheusMetric(
                            "max(process_cpu_usage_percent{iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                            legendFormat = "process"
                        )
                    )
            )
            .withPanel(
                jvmPanelTemplate(appName, "heap", appDatasource, YAxisFormat.BYTES)
                    .withMetric(
                        PrometheusMetric(
                            "max(jvm_memory_bytes_used{area=\"heap\", iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                            legendFormat = "used (max)"
                        )
                    )
                    .withMetric(
                        PrometheusMetric(
                            "avg(jvm_memory_bytes_used{area=\"heap\", iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                            legendFormat = "used (average)"
                        )
                    )
                    .withMetric(
                        PrometheusMetric(
                            "max(jvm_memory_bytes_max{area=\"heap\", iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                            legendFormat = "max (max)"
                        )
                    )
            )
            .withPanel(
                jvmPanelTemplate(appName, "pools", appDatasource, YAxisFormat.BYTES)
                    .withMetric(
                        PrometheusMetric(
                            "max(jvm_memory_pool_bytes_used{pool=~\"CodeHeap.*\", iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                            legendFormat = "code-cache used (max)"
                        )
                    )
                    .withMetric(
                        PrometheusMetric(
                            "max(jvm_memory_pool_bytes_used{pool=\"Compressed Class Space\", iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                            legendFormat = "compressed class space used (max)"
                        )
                    )
                    .withMetric(
                        PrometheusMetric(
                            "max(jvm_memory_pool_bytes_used{pool=\"Metaspace\", iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                            legendFormat = "metaspace used (max)"
                        )
                    )
            )
            .withPanel(
                jvmPanelTemplate(appName, "garbage collection", appDatasource)
                    .withMetric(
                        PrometheusMetric(
                            "max(rate(jvm_gc_collection_seconds_count{iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"}[2m]))",
                            legendFormat = "cycles (max)"
                        )
                    )
                    .withMetric(
                        PrometheusMetric(
                            "avg(rate(jvm_gc_collection_seconds_count{iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"}[2m]))",
                            legendFormat = "cycles (average)"
                        )
                    )
                    .withMetric(
                        PrometheusMetric(
                            "max(rate(jvm_gc_collection_seconds_sum{iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"}[2m])) * 1000",
                            legendFormat = "ms taken (max)"
                        )
                    )
                    .withMetric(
                        PrometheusMetric(
                            "avg(rate(jvm_gc_collection_seconds_sum{iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"}[2m])) * 1000",
                            legendFormat = "ms taken (average)"
                        )
                    )
            )
            .withPanel(
                jvmPanelTemplate(appName, "eden space", appDatasource, YAxisFormat.BYTES)
                    .withMetric(
                        PrometheusMetric(
                            "max(jvm_memory_pool_bytes_used{pool=\"G1 Eden Space\", iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                            legendFormat = "eden space used (max)"
                        )
                    )
                    .withMetric(
                        PrometheusMetric(
                            "avg(jvm_memory_pool_bytes_used{pool=\"G1 Eden Space\", iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                            legendFormat = "eden space used (average)"
                        )
                    )
                    .withMetric(
                        PrometheusMetric(
                            "max(jvm_memory_pool_bytes_committed{pool=\"G1 Eden Space\", iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                            legendFormat = "eden space committed (max)"
                        )
                    )
                    .withMetric(
                        PrometheusMetric(
                            "max(jvm_memory_pool_bytes_max{pool=\"G1 Eden Space\", iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                            legendFormat = "eden space max (max)"
                        )
                    )
            )
            .withPanel(
                jvmPanelTemplate(appName, "survivor space", appDatasource, YAxisFormat.BYTES)
                    .withMetric(
                        PrometheusMetric(
                            "max(jvm_memory_pool_bytes_used{pool=\"G1 Survivor Space\", iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                            legendFormat = "survivor space used (max)"
                        )
                    )
                    .withMetric(
                        PrometheusMetric(
                            "avg(jvm_memory_pool_bytes_used{pool=\"G1 Survivor Space\", iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                            legendFormat = "survivor space used (average)"
                        )
                    )
                    .withMetric(
                        PrometheusMetric(
                            "max(jvm_memory_pool_bytes_committed{pool=\"G1 Survivor Space\", iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                            legendFormat = "survivor space committed (max)"
                        )
                    )
                    .withMetric(
                        PrometheusMetric(
                            "max(jvm_memory_pool_bytes_max{pool=\"G1 Survivor Space\", iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                            legendFormat = "survivor space max (max)"
                        )
                    )
            )
            .withPanel(
                jvmPanelTemplate(appName, "old gen space", appDatasource, YAxisFormat.BYTES)
                    .withMetric(
                        PrometheusMetric(
                            "max(jvm_memory_pool_bytes_used{pool=\"G1 Old Gen\", iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                            legendFormat = "old gen used (max)"
                        )
                    )
                    .withMetric(
                        PrometheusMetric(
                            "avg(jvm_memory_pool_bytes_used{pool=\"G1 Old Gen\", iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                            legendFormat = "old gen used (average)"
                        )
                    )
                    .withMetric(
                        PrometheusMetric(
                            "max(jvm_memory_pool_bytes_committed{pool=\"G1 Old Gen\", iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                            legendFormat = "old gen committed (max)"
                        )
                    )
                    .withMetric(
                        PrometheusMetric(
                            "max(jvm_memory_pool_bytes_max{pool=\"G1 Old Gen\", iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                            legendFormat = "old gen max (max)"
                        )
                    )
            ).withPanel(
                jvmPanelTemplate(appName, "load", appDatasource)
                    .withMetric(
                        PrometheusMetric(
                            "max(system_load{iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                            legendFormat = "load (max)"
                        )
                    )
                    .withMetric(
                        PrometheusMetric(
                            "avg(system_load{iris_io_cf_app_name=~\"$appName(-qa)?\", instance=~\"\$p_instance\", iris_io_cf_app_space=\"\$p_environment\"})",
                            legendFormat = "load (average)"
                        )
                    )
            )

    private fun jvmPanelTemplate(
        appName: String,
        name: String,
        appDatasource: Datasource,
        yAxisFormat: YAxisFormat = YAxisFormat.SHORT
    ) =
        GenericPanel(
            "$appName - $name",
            yaxisFormats = listOf(yAxisFormat, yAxisFormat),
            span = CustomSpan(4),
            fill = 1,
            datasource = appDatasource.name,
            grid = PanelGrid(leftMin = 0)
        )

    private fun panelTemplate(
        appName: String,
        name: String,
        appDatasource: Datasource,
        yAxisFormat: YAxisFormat = YAxisFormat.SHORT,
        fill: Int = 4,
        grid: PanelGrid = PanelGrid(leftMin = 0)
    ) =
        GenericPanel(
            "$appName - $name",
            yaxisFormats = listOf(yAxisFormat, yAxisFormat),
            span = CustomSpan(6),
            fill = fill,
            datasource = appDatasource.name,
            grid = grid
        )

}
