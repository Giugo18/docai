# 1) Compilazione dell'app Angular
FROM node:24-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npx ng build --configuration production

# 2) Server web con i soli file compilati
FROM nginx:1.29-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
# All'avvio nginx genera config.json dal template, con le variabili
# d'ambiente del container (es. KEYCLOAK_URL), dentro la cartella del sito
COPY config.json.template /etc/nginx/templates/config.json.template
ENV NGINX_ENVSUBST_OUTPUT_DIR=/usr/share/nginx/html
EXPOSE 80