# Microsserviço Gerador de Mapas do Minecraft

Um microsserviço backend de alta performance desenvolvido em Spring Boot, focado na geração de mapas de mundos do Minecraft (Java Edition, desde a versão 1.20 até os lançamentos de 2026, 26.1 e 26.2) a partir de uma única Seed.

Este microsserviço utiliza **JNA (Java Native Access)** para se conectar diretamente à biblioteca nativa **Cubiomes** (escrita em C). Isso permite simular com exatidão o algoritmo de geração de terrenos **Multi-Noise** moderno do Minecraft. As saídas geradas possuem paridade de 100% com o jogo vanilla e com o **Chunkbase** (nosso padrão de qualidade).

---

## 🚀 Principais Funcionalidades

* **Paridade com Vanilla & Chunkbase**: Utiliza simulação nativa em C para o gerador de biomas multi-ruído (Temperatura, Umidade, Continentalidade, Erosão, Weirdness e Profundidade), gerando resultados idênticos aos do jogo.
* **Suporte a Múltiplas Versões (1.20 a 26.2)**: Permite selecionar a versão de geração do Minecraft dinamicamente via parâmetro. Versões baseadas em ano (ex: `26.1`, `26.2`) são mapeadas automaticamente para as regras de geração correspondentes (geração da `1.21`).
* **Amostragem de Pixel Único**: Endpoint para obter ID, nome e cor hexadecimal do bioma em qualquer coordenada de bloco exata `(x, z)`.
* **Tiles de Mapa em Tempo Real**: Endpoint que gera e serve imagens de tiles PNG de 256x256 pixels com redimensionamento dinâmico baseado no fator de zoom.
* **Cache de Alta Performance**: Cache em memória com chave composta `(Seed, Version)` que armazena os endereços de memória dos geradores nativos estruturados em C. Isso evita reprocessamentos matemáticos da biblioteca nativa e renderiza os tiles em milissegundos.
* **Arquitetura Modular**: Toda a lógica de consulta foi encapsulada sob a interface `BiomeGenerator`, permitindo fácil substituição por instâncias headless de servidores modificados (ex: Fabric/Forge) no futuro.
* **Suporte a Mods via Datapacks**: Carrega arquivos de configuração JSON de datapacks de mundos (como do *Biomes O' Plenty*) para gerar mapas com suporte completo a novos biomas. A correspondência climatológica utiliza uma Kd-Tree 6D nativa de altíssima performance.
* **Documentação OpenAPI 3.0 / Swagger**: Documentação completa e interativa dos endpoints expostos pelo microsserviço.


---

## 🛠️ Arquitetura e Stack Tecnológica

* **Framework**: Spring Boot 3.3.0
* **Linguagem**: Java 17 (toolchain Eclipse Adoptium)
* **Bindings Nativos**: JNA (Java Native Access) 5.14.0
* **Biblioteca Nativa**: Cubiomes C Library (`libcubiomes.so`, compilada para Linux x86-64)
* **Gerenciador de Build**: Gradle Kotlin DSL (`build.gradle.kts`)

### Estrutura do Projeto

```
mc_map_generator/
├── build.gradle.kts           # Configuração de dependências e JVM
├── README.md                  # Documentação do projeto (este arquivo)
└── src/
    ├── main/
    │   ├── java/org/learn/minecraftmap/
    │   │   ├── MinecraftMapApplication.java   # Classe principal da aplicação
    │   │   ├── domain/
    │   │   │   ├── BiomeInfo.java             # DTO de informações do bioma
    │   │   │   └── BiomeColorMap.java         # Mapeamento e registro de cores e nomes
    │   │   ├── generator/
    │   │   │   ├── BiomeGenerator.java        # Interface do gerador de biomas
    │   │   │   └── impl/
    │   │   │       └── VanillaBiomeGenerator.java # Implementação nativa via Cubiomes
    │   │   ├── jna/
    │   │   │   └── CubiomesLibrary.java       # Interface JNA com as funções em C
    │   │   ├── service/
    │   │   │   └── BiomeMapService.java       # Serviço de renderização e lógica de negócio
    │   │   └── controller/
    │   │       └── BiomeMapController.java    # Controlador REST e Swagger
    │   └── resources/
    │       ├── application.yml                # Configurações do Spring Boot
    │       └── linux-x86-64/
    │           └── libcubiomes.so             # Biblioteca compartilhada nativa para Linux
    └── test/
        └── java/org/learn/minecraftmap/
            └── BiomeMapServiceTest.java       # Testes de integração JUnit 5
```

---

## ⚡ Gerenciamento de Memória Nativa e Thread-Safety

A integração com o Cubiomes funciona instanciando dinamicamente a estrutura `Generator` escrita em C.

1. **Alocação do Struct**: O Java aloca exatamente `27.592` bytes de memória nativa usando a classe `com.sun.jna.Memory` do JNA. O ciclo de vida dessa memória nativa é gerenciado pelo coletor de lixo (GC) do Java, sendo liberada automaticamente assim que o objeto Java de referência é limpo.
2. **Inicialização e Seed**:
   * `setupGenerator(Pointer g, int mc, int flags)` inicializa as oitavas e parâmetros de ruído para a versão do Minecraft desejada.
   * `applySeed(Pointer g, int dim, long seed)` configura o gerador para a seed informada na dimensão do Overworld (`dim = 0`).
3. **Cache de Ponteiros**:
   * Um `ConcurrentHashMap` armazena o ponteiro configurado de cada gerador para a tupla `(Seed, Version)`.
   * As funções de amostragem (`getBiomeAt`) aceitam um ponteiro constante (`const Generator*`) e realizam operações apenas de leitura (stateless), tornando-as nativamente thread-safe para requisições concorrentes.

---

## 🌐 Endpoints da API

### 1. Amostragem de Bioma (Pixel Único)
Retorna os detalhes do bioma gerado em uma coordenada exata.

* **URL**: `/api/v1/biome`
* **Método**: `GET`
* **Parâmetros de Query**:
  * `seed` (Long, Obrigatório): A seed do mundo. (ex: `123456`)
  * `version` (String, Opcional): A versão do Minecraft. Padrão: `1.20`. Suporta do `1.20` ao `1.20.6`, `1.21`, `26.1`, `26.2`.
  * `x` (Int, Obrigatório): Coordenada X do bloco.
  * `z` (Int, Obrigatório): Coordenada Z do bloco.
* **Resposta de Sucesso**: `200 OK` (JSON)
  ```json
  {
    "id": 2,
    "name": "desert",
    "hexColor": "#fae2a2"
  }
  ```

---

### 2. Renderização de Tile (PNG)
Gera uma imagem PNG de 256x256 pixels representando o tile de biomas do mapa.

* **URL**: `/api/v1/map/tile`
* **Método**: `GET`
* **Parâmetros de Query**:
  * `seed` (Long, Obrigatório): A seed do mundo.
  * `version` (String, Opcional): A versão do Minecraft. Padrão: `1.20`.
  * `zoom` (Int, Obrigatório): Nível de zoom do mapa. No zoom `8`, cada pixel corresponde a exatamente 1 bloco (escala 1:1). Níveis de zoom menores reduzem a resolução (zoom-out).
  * `tx` (Int, Obrigatório): Coordenada horizontal do tile.
  * `ty` (Int, Obrigatório): Coordenada vertical do tile.
* **Resposta de Sucesso**: `200 OK`
  * **Headers**: `Content-Type: image/png`, `Cache-Control: no-cache, no-store, must-revalidate`
  * **Corpo**: Dados binários da imagem PNG.

---

## 🌲 Suporte a Mods (Datapacks & Kd-Tree)

O microsserviço agora possui integração completa para suportar mods que alteram ou adicionam novos biomas à geração do Overworld (como **Biomes O' Plenty**).

### Como Funciona:
1. **Leitura de Datapacks**: O microsserviço lê o arquivo de configuração JSON correspondente ao gerador do mundo multi-noise (ex: `overworld.json` do datapack).
2. **Árvore Kd-Tree 6D**: Para realizar a busca do bioma mais próximo no espaço climatológico sem comprometer a performance, o backend constrói uma árvore de partição espacial **Kd-Tree de 6 dimensões** (temperatura, umidade, continentalidade, erosão, profundidade e estranheza).
3. **Registro Dinâmico**: Biomas não-vanilla são registrados dinamicamente pelo C com IDs começando a partir de `200`. Nomes amigáveis e cores determinísticas (calculadas via hash sobre o nome do bioma) são resolvidos em tempo de execução.

### Configuração no Spring Boot:
Você pode configurar o caminho para o datapack JSON no arquivo `src/main/resources/application.yml` ou através de variáveis de ambiente:

```yaml
cubiomes:
  datapack-path: "/caminho/para/o/seu/overworld.json"
```

O ciclo de vida da memória nativa da árvore Kd-Tree é gerenciado de forma segura pelo `CustomDatapackManager` durante a inicialização e encerramento do Spring Boot.

---

## 📦 Como Compilar e Executar

### Pré-requisitos
* **Java**: OpenJDK 17 (ou superior)
* **Sistema Operacional**: Linux x86-64 (necessário para carregar a biblioteca nativa `libcubiomes.so`)

### 1. Executar os Testes Unitários
Verifique a compilação, o carregamento correto da biblioteca nativa e as asserções de biomas:
```bash
./gradlew test --no-daemon
```

### 2. Iniciar a Aplicação
Inicie o microsserviço localmente:
```bash
./gradlew bootRun --no-daemon
```
O servidor será inicializado na porta `8080`.

### 3. Documentação Swagger UI
Com o servidor rodando, acesse a interface interativa do Swagger para testar os endpoints diretamente do navegador:
* **Swagger UI URL**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
* **Documentação OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 🧪 Exemplos de Chamadas (cURL)

**Amostragem de bioma em (0,0) na seed `123456` na versão `26.2`:**
```bash
curl -s "http://localhost:8080/api/v1/biome?seed=123456&x=0&z=0&version=26.2"
# Retorno: {"id":2,"name":"desert","hexColor":"#fae2a2"}
```

**Amostragem de bioma em (100,100) na seed `123` na versão `1.20`:**
```bash
curl -s "http://localhost:8080/api/v1/biome?seed=123&x=100&z=100&version=1.20"
# Retorno: {"id":5,"name":"taiga","hexColor":"#0b4d2c"}
```

**Baixar imagem de tile de biomas em tx=0, ty=0 no zoom=8 na seed `987654321` na versão `26.2`:**
```bash
curl -o tile.png "http://localhost:8080/api/v1/map/tile?seed=987654321&zoom=8&tx=0&ty=0&version=26.2"
```
