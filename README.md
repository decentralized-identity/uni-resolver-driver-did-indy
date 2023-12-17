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
* `uniresolver_driver_did_indy_poolConfigs`: A semi-colon-separated list of Indy network names and pool configuration files.
* `uniresolver_driver_did_indy_poolVersions`: A semi-colon-separated list of Indy network names and pool protocol versions.
* `uniresolver_driver_did_indy_walletNames`: A semi-colon-separated list of Indy network names and wallet names.
* `uniresolver_driver_did_indy_submitterDidSeeds`: A semi-colon-separated list of Indy network names and seeds for submitter DIDs.

## Driver Output Metadata

The driver returns the following metadata in addition to a DID document:

* `nymResponse`: Response to the Indy `GET_NYM` operation, including `txnTime`, `state_proof`, and other information.
* `attrResponse`: Response to the Indy `GET_ATTR` operation, including `txnTime`, `state_proof`, and other information.
