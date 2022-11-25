# Grafana Dashboard

This uses [Grafanarama](https://github.com/springernature/grafanarama) to generate and upload a Grafana dashboard for the project.

## Running

You'll need to set the following env vars:

* `GF_URL` - the URL of the Grafana instance's API, e.g. https://coco-grafana.public.springernature.app/api
* `GF_API_KEY` - the API key for the Grafana instance, see `<your grafana>/org/apikeys`

Make sure you edit `Main.kt` to point at your Prometheus instance, app name, and CF space.

Then just run `./build`.
