#!/usr/bin/env bash
# Smoke REST contra uma API em execução: saúde, OpenAPI, login, papéis, pauta, sessão, voto do
# associado autenticado, duplicidade, acesso anônimo, método não suportado e rota inexistente.
# Uso: scripts/smoke-test.sh <base-url> [gestor] [eleitor] [senha]
# Códigos de saída: 0 aprovado; 1 falha; 2 uso/configuração.
set -uo pipefail
base="${1:-}"; manager="${2:-gestora}"; voter="${3:-associado-1}"; password="${4:-${SMOKE_PASSWORD:-coopvote-local}}"
[[ "$base" =~ ^https?:// ]] || { echo "uso: $0 <base-url> [gestor] [eleitor] [senha]" >&2; exit 2; }
base="${base%/}"
fail() { echo "FALHA: $*" >&2; exit 1; }
json() { python3 -c "import sys,json; d=json.load(sys.stdin); print($1)"; }
code() { curl -sS -o /dev/null -w '%{http_code}' "$@"; }
login() { curl -sS -X POST "$base/api/v1/auth/login" -H 'Content-Type: application/json' -d "{\"associateId\":\"$1\",\"password\":\"$2\"}" | json 'd["accessToken"]' 2>/dev/null; }

health=$(curl -sS "$base/actuator/health" | json 'd["status"]') || fail "health indisponível"
[ "$health" = "UP" ] || fail "health=$health"
openapi=$(curl -sS "$base/api-docs" | json 'd["openapi"]') || fail "OpenAPI indisponível"
[[ "$openapi" == 3.* ]] || fail "openapi=$openapi"
[ "$(code "$base/api/v1/agendas/00000000-0000-0000-0000-000000000000/result")" = "401" ] || fail "acesso anônimo não é 401"

mtoken=$(login "$manager" "$password"); [ -n "$mtoken" ] || fail "login do gestor"
vtoken=$(login "$voter" "$password"); [ -n "$vtoken" ] || fail "login do eleitor"
[ "$(code -X POST "$base/api/v1/auth/login" -H 'Content-Type: application/json' -d "{\"associateId\":\"$manager\",\"password\":\"senha-errada\"}")" = "401" ] || fail "senha errada não é 401"

run_id=$(date +%s%N)
agenda=$(curl -sS -X POST "$base/api/v1/agendas" -H "Authorization: Bearer $mtoken" -H 'Content-Type: application/json' -d "{\"title\":\"Smoke $run_id\"}" | json 'd["id"]') || fail "cadastro da pauta"
[ "$(code -X POST "$base/api/v1/agendas" -H "Authorization: Bearer $vtoken" -H 'Content-Type: application/json' -d '{"title":"x"}')" = "403" ] || fail "eleitor cadastrou pauta"
[ "$(code -X POST "$base/api/v1/agendas/$agenda/voting-sessions" -H "Authorization: Bearer $mtoken" -H 'Content-Type: application/json' -d '{"durationMinutes":5}')" = "201" ] || fail "abertura da sessão"
vote=$(curl -sS -X POST "$base/api/v1/agendas/$agenda/votes" -H "Authorization: Bearer $vtoken" -H 'Content-Type: application/json' -d '{"vote":"Sim"}') || fail "voto"
[ "$(echo "$vote" | json 'd["associateId"]')" = "$voter" ] || fail "voto não vinculado ao eleitor autenticado"
dup=$(curl -sS -X POST "$base/api/v1/agendas/$agenda/votes" -H "Authorization: Bearer $vtoken" -H 'Content-Type: application/json' -d '{"vote":"Não"}' | json 'd.get("code")')
[ "$dup" = "DUPLICATE_VOTE" ] || fail "duplicidade=$dup"
receipt=$(curl -sS "$base/api/v1/agendas/$agenda/votes/me" -H "Authorization: Bearer $vtoken" | json 'd["vote"]')
[ "$receipt" = "Sim" ] || fail "recibo=$receipt"
partial=$(curl -sS "$base/api/v1/agendas/$agenda/result" -H "Authorization: Bearer $vtoken" | json 'str(d.get("yesVotes"))+" "+d["result"]')
[ "$partial" = "None IN_PROGRESS" ] || fail "parcial do eleitor exposta: $partial"
manager_view=$(curl -sS "$base/api/v1/agendas/$agenda/result" -H "Authorization: Bearer $mtoken" | json 'str(d["yesVotes"])+"/"+str(d["noVotes"])')
[ "$manager_view" = "1/0" ] || fail "parcial do gestor=$manager_view"
[ "$(code -X DELETE "$base/api/v1/agendas" -H "Authorization: Bearer $mtoken")" = "405" ] || fail "método não suportado"
[ "$(code "$base/api/v1/does-not-exist" -H "Authorization: Bearer $mtoken")" = "404" ] || fail "rota inexistente"
echo "PASSED base=$base agenda=$agenda eleitor=$voter duplicidade=$dup parcial-gestor=$manager_view"
