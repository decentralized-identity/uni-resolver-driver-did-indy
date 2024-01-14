![DIF Logo](https://raw.githubusercontent.com/decentralized-identity/universal-resolver/master/docs/logo-dif.png)

# Universal Resolver Driver: did:indy

This is a [Universal Resolver](https://github.com/decentralized-identity/universal-resolver/) driver for **did:indy** identifiers.

## Specifications

* [Decentralized Identifiers](https://www.w3.org/TR/did-core/)
* [DID Method Specification](https://hyperledger.github.io/indy-did-method/)

## Example DIDs

```
did:indy:sovrin:WRfXPg8dantKVubE3HX8pw
did:indy:sovrin:staging:WRfXPg8dantKVubE3HX8pw
```

## Build and Run (Docker)

```
docker build -f ./docker/Dockerfile . -t universalresolver/driver-did-indy
docker run -p 8080:8080 universalresolver/driver-did-indy
curl -X GET http://localhost:8080/1.0/identifiers/did:indy:sovrin:WRfXPg8dantKVubE3HX8pw
```

## Build (native Java)

Maven build:

    mvn clean install

## Driver Environment Variables

The driver recognizes the following environment variables:

* `uniresolver_driver_did_indy_libIndyPath`: The path to the Indy SDK library.
* `uniresolver_driver_did_indy_openParallel`: Whether to open Indy pools in parallel threads. This speeds up startup, but may consume more memory.
* `uniresolver_driver_did_indy_poolConfigs`: A semi-colon-separated list of Indy network names and pool configuration files.
* `uniresolver_driver_did_indy_poolVersions`: A semi-colon-separated list of Indy network names and pool protocol versions.
* `uniresolver_driver_did_indy_walletNames`: A semi-colon-separated list of Indy network names and wallet names.
* `uniresolver_driver_did_indy_submitterDidSeeds`: A semi-colon-separated list of Indy network names and seeds for submitter DIDs.

## Driver Output Metadata

The driver returns the following metadata in addition to a DID document:

* `nymResponse`: Response to the Indy `GET_NYM` operation, including `txnTime`, `state_proof`, and other information.
* `attrResponse`: Response to the Indy `GET_ATTR` operation, including `txnTime`, `state_proof`, and other information.


## Usage with local Von-Network

For development and testing it may be useful to run a local instance of the [von-network](https://github.com/bcgov/von-network) in a Docker container
and resolve DIDs with this Driver.
#### Setting up the Von-Network
[This Tutorial](https://github.com/bcgov/von-network/blob/main/docs/UsingVONNetwork.md) explains how to set up the 
von-network. Since the Resolver will run in a separate Docker container, the von-network needs to use the IP address
of the host machine for the nodes, not the Docker Host IP.
This can be achieved by running `./manage start <host_ip>` where `<host_ip>` is the IP address of the host machine. <br/>
**Note**: If you followed the tutorial and already started the network, you need to run `./manage down` first to delete all
the data in the ledger. After this you can run `./manage start <host_ip>`.<br/>
After the von-network started, the genesis file is available at `http://localhost:9000/genesis`.

#### Usage with this Driver
Copy the genesis file into a new file (e.g. `von-local.txn`) in the `sovrin` directory of this driver.
Open `docker/Dockerfile` and append the environment variables like this:
```agsl
ENV uniresolver_driver_did_indy_libIndyPath=
ENV uniresolver_driver_did_indy_poolConfigs=sovrin;./sovrin/sovrin.txn;...;von:local;./sovrin/von-local.txn
ENV uniresolver_driver_did_indy_poolVersions=sovrin;2;...;von:local;2
ENV uniresolver_driver_did_indy_walletNames=sovrin;w1;...;von:local;w19
ENV uniresolver_driver_did_indy_submitterDidSeeds=sovrin;_;...;von:local;_
```
A DID in the von-network can then be queried with `curl -X GET http://localhost:8080/1.0/identifiers/did:indy:von:local:<DID>`
where `<DID>` is the DID that should be resolved.

#### Usage with the Universal Resolver
Create a new directory `networks` in the Resolver directory. Create a file in this directory called `von-local.txn` and
copy the genesis file in there.
To make this file available to the Indy Driver, in the Resolver adjust `docker-compose.yml`.
Find the `driver-did-indy` service and add a volume like this:
```
  driver-did-indy:
    image: universalresolver/driver-did-indy:latest
    environment:
      ...
    ports:
      ...
    volumes:
      - ./networks/von-local.txn:/var/lib/jetty/sovrin/von-local.txn
```
This copies the `von-local.txn` file to the driver container. Finally, adjust the `.env` file in the Resolver directory.
Find the entries for `uniresolver_driver_did_indy_poolConfigs`, `uniresolver_driver_did_indy_poolVersions`, 
`uniresolver_driver_did_indy_walletNames` and `uniresolver_driver_did_indy_submitterDidSeeds` and append them like this:
```
uniresolver_driver_did_indy_libIndyPath=
uniresolver_driver_did_indy_poolConfigs=sovrin;./sovrin/sovrin.txn;...;von:local;./sovrin/von-local.txn
uniresolver_driver_did_indy_poolVersions=sovrin;2;...;von:local;2
uniresolver_driver_did_indy_walletNames=sovrin;w1;...;von:local;w13
uniresolver_driver_did_indy_submitterDidSeeds=sovrin;_;...;von:local;_
```
A DID in the von-network can then be queried with `curl -X GET http://localhost:8080/1.0/identifiers/did:indy:von:local:<DID>`
where `<DID>` is the DID that should be resolved.