#!/bin/bash

# Kolory
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}╔════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  HardGamezEngine - Full GitHub Setup     ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════╝${NC}\n"

# Pytaj o dane użytkownika
echo -e "${YELLOW}Podaj swoje dane do konfiguracji Git:${NC}"
read -p "Imię i nazwisko (np. Jan Kowalski): " git_name
read -p "Email (np. jan@example.com): " git_email
echo ""

# 1. Konfiguracja Git globalnie
echo -e "${BLUE}[1/9]${NC} Konfiguracja Git..."
git config --global user.name "$git_name"
git config --global user.email "$git_email"
git config --global init.defaultBranch main
echo -e "${GREEN}✓ Git skonfigurowany${NC}"
echo -e "  Nazwa: $git_name"
echo -e "  Email: $git_email\n"

# 2. Usuń stary Git
echo -e "${BLUE}[2/9]${NC} Czyszczenie starej konfiguracji..."
rm -rf .git
git remote remove origin 2>/dev/null || true
echo -e "${GREEN}✓ Wyczyszczono${NC}\n"

# 3. Utwórz .gitignore
echo -e "${BLUE}[3/9]${NC} Tworzenie .gitignore..."
cat > .gitignore << 'EOF'
# Gradle
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar
!**/src/main/**/build/
!**/src/test/**/build/

# IntelliJ IDEA
.idea/
*.iws
*.iml
*.ipr
out/
!**/src/main/**/out/
!**/src/test/**/out/

# Eclipse
.classpath
.project
.settings/
bin/

# macOS
.DS_Store
.AppleDouble
.LSOverride

# Log files
*.log

# Package Files (wyjątek dla gradle wrapper)
*.jar
*.war
*.nar
*.ear
*.zip
*.tar.gz
*.rar
!gradle/wrapper/*.jar

# Virtual machine crash logs
hs_err_pid*
replay_pid*

# Temporary files
*.tmp
*.bak
*.swp
*~
EOF
echo -e "${GREEN}✓ .gitignore utworzony${NC}\n"

# 4. Utwórz README.md (jeśli nie istnieje)
echo -e "${BLUE}[4/9]${NC} Sprawdzanie README.md..."
if [ ! -f README.md ]; then
    echo -e "${YELLOW}⚠ README.md nie istnieje, tworzę podstawowy...${NC}"
    cat > README.md << 'EOF'
# HardGamezEngine

**HardGamezEngine** to podstawowy plugin typu API, który dostarcza zaawansowane narzędzia i funkcjonalności dla pluginów z rodziny HardGamez. Plugin został zaprojektowany z myślą o maksymalnej elastyczności i wygodzie konfiguracji, stanowiąc fundament dla wszystkich mini-gier w ekosystemie HardGamez Studio.

## 🎯 Główne funkcjonalności

- Zarządzanie aktualizacjami
- Zarządzanie konfiguracją dla Paper i Velocity
- System językowy
- Zarządzanie danymi graczy
- Narzędzia deweloperskie

## 📋 Wymagania

- Serwer Minecraft (PaperMC/Velocity)
- Java 21 lub nowsza

## 📥 Instalacja

1. Pobierz najnowszą wersję z [releases](../../releases)
2. Umieść plik `.jar` w folderze `plugins/`
3. Uruchom serwer

## 📝 Licencja

Projekt stworzony dla społeczności Minecraft ❤️
EOF
    echo -e "${GREEN}✓ README.md utworzony${NC}\n"
else
    echo -e "${GREEN}✓ README.md już istnieje${NC}\n"
fi

# 5. Inicjalizuj Git
echo -e "${BLUE}[5/9]${NC} Inicjalizacja repozytorium Git..."
git init
echo -e "${GREEN}✓ Git zainicjalizowany${NC}\n"

# 6. Dodaj pliki
echo -e "${BLUE}[6/9]${NC} Dodawanie plików do Git..."
git add .
echo -e "${GREEN}✓ Pliki dodane${NC}\n"

# 7. Commit
echo -e "${BLUE}[7/9]${NC} Tworzenie pierwszego commita..."
git commit -m "Initial commit: Multi-platform HardGamezEngine for Paper and Velocity

- Added Paper module
- Added Velocity module  
- Added Common module for shared code
- Configured Gradle multi-module project
- Added .gitignore for Java/Kotlin/Gradle projects"
echo -e "${GREEN}✓ Commit utworzony${NC}\n"

# 8. Dodaj remote
echo -e "${BLUE}[8/9]${NC} Dodawanie remote GitHub..."
git remote add origin https://github.com/lakidevpl/HardGamezEngine.git
git branch -M main
echo -e "${GREEN}✓ Remote dodany${NC}\n"

# 9. Push na GitHub
echo -e "${BLUE}[9/9]${NC} Wysyłanie na GitHub..."
echo -e "${YELLOW}⚠ Możesz zostać poproszony o dane logowania${NC}"
echo -e "${YELLOW}  Username: lakidevpl${NC}"
echo -e "${YELLOW}  Password: [Użyj Personal Access Token]${NC}\n"

git push -u origin main --force

# Sprawdź wynik
if [ $? -eq 0 ]; then
    echo -e "\n${GREEN}╔════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║          ✓ SUKCES!                     ║${NC}"
    echo -e "${GREEN}║  Projekt jest na GitHub!               ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════╝${NC}\n"
    echo -e "${BLUE}📦 Twoje repozytorium:${NC}"
    echo -e "   https://github.com/lakidevpl/HardGamezEngine\n"
    echo -e "${BLUE}🔧 Konfiguracja Git:${NC}"
    echo -e "   Nazwa: $git_name"
    echo -e "   Email: $git_email\n"
    echo -e "${BLUE}📝 Następne kroki:${NC}"
    echo -e "   git status     - sprawdź status"
    echo -e "   git add .      - dodaj zmiany"
    echo -e "   git commit -m  - zapisz zmiany"
    echo -e "   git push       - wyślij na GitHub\n"
else
    echo -e "\n${RED}╔════════════════════════════════════════╗${NC}"
    echo -e "${RED}║          ✗ BŁĄD!                       ║${NC}"
    echo -e "${RED}╚════════════════════════════════════════╝${NC}\n"
    echo -e "${YELLOW}Możliwe przyczyny:${NC}"
    echo -e "  1. Nie masz dostępu do repozytorium"
    echo -e "  2. Użyłeś hasła zamiast Personal Access Token"
    echo -e "  3. Repozytorium nie istnieje\n"
    echo -e "${BLUE}Jak wygenerować Token:${NC}"
    echo -e "  1. https://github.com/settings/tokens"
    echo -e "  2. Generate new token (classic)"
    echo -e "  3. Zaznacz 'repo'"
    echo -e "  4. Użyj token jako hasła\n"
fi
