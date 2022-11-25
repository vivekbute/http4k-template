team: sn-digital
pipeline: http4k-sn-template
platform: actions
slack_channel: "#http4k-sn"

triggers:
- type: git
  shallow: true

tasks:
  - type: docker-compose
    name: build
    save_artifacts:
      - target/distributions
    save_artifacts_on_failure:
      - target/reports
    command: ./build clean build --console=plain

  - type: docker-compose
    name: integration-tests-live
    restore_artifacts: true
    command: bin/run-integration-tests live

#  - type: docker-compose
#    name: upload-dashboard
#    command: ./build clean build --console=plain
#    vars:
#        GF_URL: https://your.grafana/api
#        GF_API_KEY: a.valid.grafana.api.key

#  - type: deploy-cf
#    name: deploy-to-live
#    api: ((cloudfoundry.api-snpaas))
#    org: ((cloudfoundry.org-snpaas))
#    username: ((cloudfoundry.username-snpaas))
#    password: ((cloudfoundry.password-snpaas))
#    space: http4ksn-template
#    manifest: etc/cf/manifest-live.yml
#    deploy_artifact: target/distributions/http4k-sn-template.zip
