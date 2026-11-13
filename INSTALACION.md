# Guía de instalación en un equipo nuevo (Windows)

Pasos para dejar un equipo nuevo listo para trabajar en SindiNails con Claude Code. Se tarda unos 30-40 minutos, casi todo en descargas.

> **Antes de empezar, en el equipo antiguo:** sube el trabajo a GitHub (`git push origin main` y las ramas que quieras conservar). El equipo nuevo clona desde GitHub: lo que no esté subido no llega.

---

## 1. Programas

Abre **PowerShell** y ejecuta cada línea. Cuando `winget` pida permisos de administrador, acéptalos.

```powershell
winget install --id Git.Git -e
winget install --id Docker.DockerDesktop -e
winget install --id EclipseAdoptium.Temurin.21.JDK -e
winget install --id OpenJS.NodeJS.LTS -e
winget install --id Python.Python.3.12 -e
```

| Programa | Para qué |
|---|---|
| Git | Clonar el repositorio. Incluye **Git Bash**, que usa Claude Code en Windows |
| Docker Desktop | MySQL en local y los tests del backend (Testcontainers) |
| JDK 21 | Compilar y ejecutar el backend (Maven va incluido en el proyecto: `mvnw`) |
| Node.js LTS | Frontend (Vite, tests, lint) |
| Python 3.12 | El buscador de la skill `ui-ux-pro-max` |

Después:

1. **Reinicia el equipo**: Docker Desktop necesita WSL 2, y así la terminal encuentra `git`, `node`, `java` y `python`.
2. **Abre Docker Desktop** y espera a que diga que está en marcha. Debe estar abierto siempre que trabajes en el proyecto.
3. Comprueba en una terminal nueva:
   ```powershell
   git --version; java -version; node --version; npm --version; python --version; docker info --format '{{.ServerVersion}}'
   ```
   Si `python` abre la Microsoft Store, desactiva el alias en *Configuración → Aplicaciones → Configuración avanzada de aplicaciones → Alias de ejecución de aplicaciones* (`python.exe` y `python3.exe`).

## 2. Claude Code

En PowerShell:

```powershell
irm https://claude.ai/install.ps1 | iex
```

Cierra y abre la terminal, ejecuta `claude` e inicia sesión con tu cuenta.

## 3. Clonar el proyecto

En **Git Bash**, en la carpeta donde quieras el proyecto:

```bash
git clone https://github.com/marcosLOL99/Proyecto_SindiNails.git
cd Proyecto_SindiNails
git config user.name "Tu nombre"
git config user.email "tu@email.com"
```

## 4. Configuración local (`.env`)

El `.env` no está en git (lleva secretos), así que hay que crearlo en cada equipo:

```bash
cp .env.example .env
openssl rand -base64 32
```

Abre `.env` y pega el resultado de `openssl` en `JWT_SECRET_KEY=`. El resto de valores sirven tal cual para trabajar en local.

- Usa un secreto **nuevo**; no copies el de otro equipo ni el antiguo que quedó en el historial de git.
- Con un secreto distinto, las sesiones abiertas en el otro equipo no valen aquí: basta con volver a iniciar sesión en la web.

## 5. Skills de Claude Code

Las skills viven en tu carpeta de usuario (`~/.claude/skills/`), no en el repositorio. Copia este bloque entero en **Git Bash**:

```bash
TMP=$(mktemp -d)
SKILLS="characterisation-tests tdd codebase-design front-end-testing react-testing refactoring"

# Seis skills de citypaul/.dotfiles
git clone --depth 1 --filter=blob:none --sparse https://github.com/citypaul/.dotfiles "$TMP/citypaul"
git -C "$TMP/citypaul" sparse-checkout set $(for s in $SKILLS; do echo "claude/.claude/skills/$s"; done)
mkdir -p ~/.claude/skills
for s in $SKILLS; do cp -r "$TMP/citypaul/claude/.claude/skills/$s" ~/.claude/skills/; done

# ui-ux-pro-max (solo esa skill del repositorio)
git clone --depth 1 https://github.com/nextlevelbuilder/ui-ux-pro-max-skill "$TMP/uiux"
cp -r "$TMP/uiux/.claude/skills/ui-ux-pro-max" ~/.claude/skills/
# Su SKILL.md da la ruta del script para instalarla como plugin; instalada a mano, la ruta es $HOME/.claude/skills/...
sed -i 's#\${CLAUDE_PLUGIN_ROOT}/\.claude/skills/ui-ux-pro-max#$HOME/.claude/skills/ui-ux-pro-max#g' ~/.claude/skills/ui-ux-pro-max/SKILL.md

rm -rf "$TMP"
ls ~/.claude/skills
```

Deben aparecer las siete: `characterisation-tests`, `codebase-design`, `front-end-testing`, `react-testing`, `refactoring`, `tdd` y `ui-ux-pro-max`. Para qué sirve cada una está en `CLAUDE.md` ("Skills y cuándo usar cada una").

Comprueba el buscador de `ui-ux-pro-max`:

```bash
python "$HOME/.claude/skills/ui-ux-pro-max/scripts/search.py" "beauty salon booking" --design-system
```

> Se instalaron y revisaron en las versiones `cd4028d` (citypaul/.dotfiles) y `6ae86f6` (ui-ux-pro-max-skill). El bloque descarga la versión más reciente: si ha cambiado mucho, revísala antes de usarla.

## 6. Dependencias y primer arranque

```bash
# Frontend
cd frontend-react
npm install          # el aviso de que esbuild no ejecutó su script es normal
cd ..

# Base de datos (la primera vez carga backend-springboot/init-db/dump.sql)
docker compose up -d db
```

Arranca cada parte en su propia terminal:

```bash
# Terminal 1: backend en http://localhost:8080
cd backend-springboot && ./mvnw spring-boot:run

# Terminal 2: frontend en http://localhost:5173
cd frontend-react && npm run dev
```

Abre **http://localhost:5173**. Al arrancar el backend aparece un WARN sobre `fk_tipo_servicio`: es conocido (peligro nº 2 de `CLAUDE.md`) y no impide arrancar.

La base de datos de este equipo es nueva: solo tiene los datos del dump. Para tener un admin, regístrate en la web y después:

```bash
docker exec -it sindinails-db mysql -uuser -ppassword sindinails_db -e "UPDATE usuarios SET role='ADMIN' WHERE email='tu@email.com';"
```

Cierra sesión y vuelve a entrar para que el token lleve el rol.

## 7. Comprobar que todo funciona

```bash
cd backend-springboot && ./mvnw test        # con Docker abierto; tarda 1-2 minutos
cd ../frontend-react && npm run test:run && npm run lint
```

Todos los tests deben pasar. El lint tiene 24 problemas que ya existían (peligro nº 8 de `CLAUDE.md`).

---

## Qué no se copia entre equipos

| Qué | Dónde vive | Qué hacer |
|---|---|---|
| Código y `CLAUDE.md` | GitHub | `git push` en un equipo, `git pull` en el otro |
| `.env` | Solo en cada equipo | Crearlo con el paso 4 |
| Skills | `~/.claude/skills/` | Paso 5 |
| Datos de la base de datos | Volumen de Docker de cada equipo | Cada equipo tiene los suyos; parten del dump |
| Memoria de Claude Code | `~/.claude/projects/` | No se sincroniza. Las reglas del proyecto están en `CLAUDE.md`, que sí va en git |

Al cambiar de equipo: haz `git push` antes de levantarte y `git pull` al sentarte en el otro.
