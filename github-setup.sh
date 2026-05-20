#!/bin/bash

# GitHub Setup Script dla macOS (HTTPS)
# Użycie: ./github-setup.sh

echo "🚀 GitHub Setup Script dla macOS"
echo "=================================="
echo ""

# Kolory dla outputu
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 1. Sprawdzenie czy git jest zainstalowany
echo "📋 Sprawdzanie czy git jest zainstalowany..."
if ! command -v git &> /dev/null; then
    echo -e "${RED}❌ Git nie jest zainstalowany!${NC}"
    echo "Zainstaluj git: brew install git"
    exit 1
fi
echo -e "${GREEN}✓ Git jest zainstalowany${NC}"
echo ""

# 2. Konfiguracja użytkownika git
echo "👤 Konfiguracja użytkownika git"
echo "=================================="
read -p "Podaj swoje imię i nazwisko: " user_name
read -p "Podaj swój email: " user_email

git config --global user.name "$user_name"
git config --global user.email "$user_email"

echo -e "${GREEN}✓ Skonfigurowano git${NC}"
echo "  Użytkownik: $user_name"
echo "  Email: $user_email"
echo ""

# 3. Inicjalizacja repozytorium
echo "📁 Inicjalizacja repozytorium"
echo "=================================="
read -p "Podaj ścieżkę do projektu (domyślnie: $(pwd)): " project_path
project_path=${project_path:-.}

if [ ! -d "$project_path" ]; then
    echo -e "${RED}❌ Katalog nie istnieje!${NC}"
    exit 1
fi

cd "$project_path"
git init
echo -e "${GREEN}✓ Repozytorium zainicjalizowane${NC}"
echo ""

# 4. Połączenie z GitHub
echo "🔗 Połączenie z GitHub"
echo "=================================="
read -p "Podaj URL repozytorium GitHub (https://github.com/user/repo.git): " repo_url

if [ -z "$repo_url" ]; then
    echo -e "${RED}❌ URL nie może być pusty!${NC}"
    exit 1
fi

git remote add origin "$repo_url"
echo -e "${GREEN}✓ Remote 'origin' dodany${NC}"
echo ""

# 5. Pobieranie zawartości z GitHub
echo "📥 Pobieranie zawartości z GitHub..."
echo "=================================="
read -p "Czy repozytorium ma już pliki? (t/n): " has_files

if [ "$has_files" = "t" ] || [ "$has_files" = "T" ]; then
    echo "Pobieranie zawartości..."
    git fetch origin main 2>/dev/null || git fetch origin master 2>/dev/null

    # Sprawdzenie której gałęzi użyć
    if git rev-parse --verify origin/main > /dev/null 2>&1; then
        git branch -M main
        git reset --hard origin/main
        echo -e "${GREEN}✓ Pobrano zawartość (gałąź: main)${NC}"
    elif git rev-parse --verify origin/master > /dev/null 2>&1; then
        git branch -M master
        git reset --hard origin/master
        echo -e "${GREEN}✓ Pobrano zawartość (gałąź: master)${NC}"
    else
        echo -e "${YELLOW}⚠ Nie można połączyć z zdalnym repozytorium${NC}"
    fi
else
    echo -e "${GREEN}✓ Repozytorium puste, gotowe do dodania plików${NC}"
fi
echo ""

# 6. Konfiguracja GitHub Credentials (macOS Keychain)
echo "🔐 Konfiguracja dostępu GitHub"
echo "=================================="
git config --global credential.helper osxkeychain
echo -e "${GREEN}✓ Włączony macOS Keychain dla kredencjałów${NC}"
echo ""

# 7. Testowanie połączenia
echo "🧪 Testowanie połączenia"
echo "=================================="
echo "Testowanie dostępu do GitHub..."
git ls-remote origin > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Połączenie z GitHub OK${NC}"
else
    echo -e "${YELLOW}⚠ Mogą być problemy z dostępem${NC}"
    echo "  Sprawdź czy URL jest poprawny: $repo_url"
    echo "  Sprawdź czy masz dostęp do tego repozytorium"
fi
echo ""

# 8. Podsumowanie
echo "✅ Setup zakończony!"
echo "=================================="
echo ""
echo "Możesz teraz używać:"
echo -e "${GREEN}  git add .${NC}        # Dodaj zmiany"
echo -e "${GREEN}  git commit -m \"msg\"${NC}  # Zatwierdź zmiany"
echo -e "${GREEN}  git push${NC}          # Wyślij na GitHub"
echo ""
echo "Aktualna gałąź:"
git branch
echo ""
echo "Remote:"
git remote -v