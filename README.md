# 4 en Raya

Implementación del juego **4 en Raya** utilizando sockets para una arquitectura Cliente-Servidor.

---

## Tecnologías y Requisitos

Este proyecto está construido con Java y gestionado con **Maven**.

* **Java Development Kit (JDK):** Versión 17 o superior.
* **Maven**
* **Conexion local:** El servidor y cliente en local escucha por defecto en el puerto **`3000`**.
* **Conexion remota:** El  cliente usa el puerto **`433`**, el Proxmox lo redirige al puerto **`80`** para despues conectarse al juego atravez del puerto **`3000`**.

---


### Maven

Es necesario tener instalado Maven para poder compilar este proyecto, para instalarlo ejecute el siguiente codigo:

```bash
mvn clean install
```
---
##  Puesta en Marcha

Para compilar y ejecutar el proyecto, se han incluido los scripts `.\run.ps1 com.server.Main` para iniciar el servidor y `.\run.ps1 com.client.Main` para iniciar como cliente.
