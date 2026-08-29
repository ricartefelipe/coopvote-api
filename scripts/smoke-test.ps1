param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^https?://')]
    [string] $BaseUrl
)

$ErrorActionPreference = 'Stop'
$apiUrl = $BaseUrl.TrimEnd('/')
$runId = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
$associateId = "smoke-associate-$runId"

$health = Invoke-RestMethod -Uri "$apiUrl/actuator/health"
if ($health.status -ne 'UP') {
    throw "Health check returned '$($health.status)'."
}

$openApi = Invoke-RestMethod -Uri "$apiUrl/api-docs"
if (-not $openApi.openapi.StartsWith('3.')) {
    throw "OpenAPI document is not available."
}

$agendaBody = @{
    title = "Deployment smoke test $runId"
    description = 'Automated verification of the published API.'
} | ConvertTo-Json

$agenda = Invoke-RestMethod `
    -Uri "$apiUrl/api/v1/agendas" `
    -Method Post `
    -ContentType 'application/json' `
    -Body $agendaBody

$session = Invoke-RestMethod `
    -Uri "$apiUrl/api/v1/agendas/$($agenda.id)/voting-sessions" `
    -Method Post `
    -ContentType 'application/json' `
    -Body '{"durationMinutes":1}'

$voteBody = @{
    associateId = $associateId
    vote = 'Sim'
} | ConvertTo-Json

Invoke-RestMethod `
    -Uri "$apiUrl/api/v1/agendas/$($agenda.id)/votes" `
    -Method Post `
    -ContentType 'application/json' `
    -Body $voteBody | Out-Null

$result = Invoke-RestMethod -Uri "$apiUrl/api/v1/agendas/$($agenda.id)/result"
if ($result.yesVotes -ne 1 -or $result.noVotes -ne 0 -or $result.totalVotes -ne 1) {
    throw 'Vote totals do not match the smoke-test scenario.'
}

$duplicateCode = $null
try {
    Invoke-RestMethod `
        -Uri "$apiUrl/api/v1/agendas/$($agenda.id)/votes" `
        -Method Post `
        -ContentType 'application/json' `
        -Body $voteBody | Out-Null
} catch {
    if ([int] $_.Exception.Response.StatusCode -ne 409) {
        throw
    }

    $duplicateCode = ($_.ErrorDetails.Message | ConvertFrom-Json).code
}

if ($duplicateCode -ne 'DUPLICATE_VOTE') {
    throw "Duplicate-vote protection returned '$duplicateCode'."
}

[PSCustomObject]@{
    status = 'PASSED'
    health = $health.status
    openApi = $openApi.openapi
    agendaId = $agenda.id
    sessionId = $session.id
    yesVotes = $result.yesVotes
    duplicateVote = $duplicateCode
}
