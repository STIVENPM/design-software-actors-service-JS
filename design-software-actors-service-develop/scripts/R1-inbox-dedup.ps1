param(
    [Parameter(Mandatory)][string]$EnvFile,
    [string]$Project = 'plan-js-local',
    [string]$Image = 'plan-js-actors:r1',
    [ValidateSet('dedup','seed-dlq')][string]$Mode = 'dedup'
)
$ErrorActionPreference = 'Stop'
$classpath = '/root/.m2/repository/com/rabbitmq/amqp-client/5.21.0/amqp-client-5.21.0.jar:/root/.m2/repository/org/postgresql/postgresql/42.7.4/postgresql-42.7.4.jar:/root/.m2/repository/org/slf4j/slf4j-api/2.0.16/slf4j-api-2.0.16.jar'
$arg = if ($Mode -eq 'seed-dlq') { 'dlq' } else { '' }
$command = "javac -cp $classpath -d /tmp /scripts/R1InboxDedup.java && java -cp /tmp:$classpath R1InboxDedup $arg"
& docker run --rm --network "${Project}_default" --env-file $EnvFile -v "${PSScriptRoot}:/scripts:ro" $Image sh -c $command
if ($LASTEXITCODE -ne 0) { throw "R1 AMQP verification failed ($LASTEXITCODE)" }
