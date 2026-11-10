#!/usr/bin/env bash
# Envia o projeto Musicin para o GitHub (rode dentro do Termux, na pasta do projeto)
set -e
cd "$(dirname "$0")"

git init -b main >/dev/null 2>&1 || git init
git add -A
git commit -m "Musicin: primeira versão" || true

if command -v gh >/dev/null 2>&1; then
  gh repo create Musicin --public --source=. --remote=origin --push
  echo
  echo "Pronto! Abra a aba Actions do repositório Musicin no GitHub para acompanhar o build."
else
  echo
  echo "O comando 'gh' não está instalado. Duas opções:"
  echo "  1) Rode: pkg install gh   e depois:  bash enviar.sh"
  echo "  2) Crie um repositório vazio chamado Musicin no site do GitHub e rode:"
  echo "       git remote add origin https://github.com/SEU_USUARIO/Musicin.git"
  echo "       git push -u origin main"
fi
