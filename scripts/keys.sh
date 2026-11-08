#!/bin/sh
# Секреты для docker compose: создаются один раз случайными и лежат в томе `keys` (не в git, не в образах).
# Свои значения можно задать в .env — тогда сервисы возьмут их, а не эти файлы.
#   db_password      пароль PostgreSQL (пользователь lekseradar)
#   jwt_secret       подпись токенов входа (JWT_SECRET)
#   credentials_key  AES-256 ключ доступов к платформам (CREDENTIALS_KEY); не терять, иначе подключения заново
#   collector_key    общий ключ бэкенда и collector (COLLECTOR_KEY)
# Посмотреть: docker compose run --rm --entrypoint cat keys /keys/db_password
set -eu
umask 022
make() {  # имя, команда
  if [ ! -s "/keys/$1" ]; then
    sh -c "$2" | tr -d '\n' > "/keys/$1.tmp" && mv "/keys/$1.tmp" "/keys/$1"
    echo "keys: created $1"
  fi
  chmod 0444 "/keys/$1"
}
make db_password     "head -c 24 /dev/urandom | od -An -tx1 | tr -d ' \n'"
make jwt_secret      "head -c 48 /dev/urandom | base64 | tr -d '\n'"
make credentials_key "head -c 32 /dev/urandom | base64 | tr -d '\n'"
make collector_key   "head -c 32 /dev/urandom | od -An -tx1 | tr -d ' \n'"
