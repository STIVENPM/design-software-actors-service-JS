param(
    [Parameter(Mandatory)][string]$ComposeFile,
    [Parameter(Mandatory)][string]$OverrideFile,
    [Parameter(Mandatory)][string]$EnvFile,
    [Parameter(Mandatory)][string]$CredentialsFile,
    [Parameter(Mandatory)][guid]$InstructorId,
    [string]$Project = 'plan-js-local',
    [string]$GatewayUrl = 'http://127.0.0.1:18088',
    [string]$StartDatetime = '2026-11-04T08:00:00'
)
$ErrorActionPreference = 'Stop'
$compose = @('compose','--env-file',$EnvFile,'-f',$ComposeFile,'-f',$OverrideFile,'-p',$Project)
$brokerStopped = $false
$eventId = $null
$businessId = $null
$postgres = "$Project-postgres-1"
$kafka = "$Project-kafka-1"
function Query-Sql([string]$sql) {
    $answer = $sql | docker exec -i $postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -At'
    if ($LASTEXITCODE -ne 0) { throw "PostgreSQL query failed" }
    return $answer
}
try {
    & docker @compose stop kafka | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not stop Kafka' }
    $brokerStopped = $true
    $creds = Get-Content $CredentialsFile -Raw | ConvertFrom-Json
    $loginBody = @{email=$creds.email;password=$creds.password} | ConvertTo-Json -Compress
    $login = Invoke-RestMethod -Uri "$GatewayUrl/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body $loginBody
    $token = $login.access_token
    if (-not $token) { $token = $login.accessToken }
    if (-not $token) { throw 'IAM did not return an access token' }
    $end = ([datetime]::Parse($StartDatetime)).AddHours(2).ToString('yyyy-MM-ddTHH:mm:ss')
    $body = @{exception_type='TRAINING';start_datetime=$StartDatetime;end_datetime=$end;description='R1 broker-outage verification'} | ConvertTo-Json -Compress
    $result = Invoke-WebRequest -Uri "$GatewayUrl/api/v1/instructors/$InstructorId/availability-exceptions" -Method Post -Headers @{Authorization="Bearer $token"} -ContentType 'application/json' -Body $body
    if ($result.StatusCode -ne 201) { throw "Expected HTTP 201, got $($result.StatusCode)" }
    $businessId = [guid](($result.Content | ConvertFrom-Json).id)
    $row = Query-Sql "select event_id,status from actors_tracking.outbox_event where aggregate_id='$businessId' order by created_at desc limit 1;"
    if (-not $row) { throw 'Business write has no corresponding outbox row' }
    $parts = $row -split '\|'
    $eventId = [guid]$parts[0]
    if ($parts[1] -notin @('PENDING','SENDING')) { throw "Unexpected outage status $($parts[1])" }
    Write-Output "BROKER_DOWN_OK business=$businessId event=$eventId status=$($parts[1])"
    & docker @compose restart actors-js | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Could not restart Actors' }
} finally {
    if ($brokerStopped) {
        & docker @compose start kafka | Out-Null
        if ($LASTEXITCODE -ne 0) { Write-Error 'Kafka could not be restored' }
    }
}
if (-not $eventId) { throw 'Chaos scenario did not produce an event' }
$deadline = (Get-Date).AddMinutes(3)
do {
    Start-Sleep -Seconds 3
    $row = Query-Sql "select status,attempts from actors_tracking.outbox_event where event_id='$eventId';"
    $parts = $row -split '\|'
    if ($parts[0] -eq 'DEAD') { throw "Event exhausted retries: $eventId" }
} until ($parts[0] -eq 'PUBLISHED' -or (Get-Date) -gt $deadline)
if ($parts[0] -ne 'PUBLISHED') { throw "Event was not published after broker recovery: $eventId" }
$lines = @(docker exec $kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic actors-events --from-beginning --timeout-ms 5000 --max-messages 100 2>$null)
$matching = @($lines | Where-Object { $_ -match [regex]::Escape($eventId.ToString()) })
if ($matching.Count -ne 1) { throw "Expected one Kafka message for $eventId; found $($matching.Count)" }
Write-Output "RECOVERY_OK business=$businessId event=$eventId attempts=$($parts[1]) kafka_matches=1"
