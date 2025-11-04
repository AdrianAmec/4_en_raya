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
sudo apt update
```
```bash
sudo apt install maven
```
---
##  Puesta en Marcha

**Iniciar Servidor**

Para crear un servidor en local ejecuta el siguiente comando para hostear la partida en local

* **Windows** `.\run.ps1 com.server.Main`
* **Linux** `.\run.sh com.server.Main`


**Iniciar Clientes (jugadores)**

* **Windows**  `.\run.ps1 com.client.Main`
* **Linux**  `.\run.sh com.client.Main`



**Conectarse al localhost**

* Seleciona el boton `Local` (*Protocolo: `ws`, Server IP: `localhost`, Puerto: `3000`*) y `Connect`



**Para conecectarse a partidas en linea (proxmox)**

* Selecciona el boton `Proxmox` (*Protocolo: `wss`, Server IP: `aescalantecarbajo.ieti.site`, Puerto: `443`*) y `Connect`
