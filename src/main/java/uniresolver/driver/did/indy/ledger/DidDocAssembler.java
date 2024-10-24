package uniresolver.driver.did.indy.ledger;

import foundation.identity.did.DID;
import foundation.identity.did.DIDDocument;
import foundation.identity.did.VerificationMethod;
import foundation.identity.jsonld.JsonLDUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uniresolver.ResolutionException;
import uniresolver.driver.did.indy.crypto.VerkeyUtil;

import java.net.URI;
import java.util.*;

public class DidDocAssembler {

    public static final List<URI> DIDDOCUMENT_CONTEXTS = List.of(
            URI.create("https://w3id.org/security/suites/ed25519-2018/v1")
    );

    public static final String[] DIDDOCUMENT_VERIFICATIONMETHOD_KEY_TYPES = new String[] { "Ed25519VerificationKey2018" };

    private static final Logger log = LoggerFactory.getLogger(DidDocAssembler.class);

    public static DIDDocument assembleDIDDocument(DID did, TransactionData nymTransactionData, TransactionData attribTransactionData) throws ResolutionException {

        // check if transactions are complete

        if (nymTransactionData == null || ! nymTransactionData.isCompleteForNym()) {
            throw new ResolutionException("Not complete for NYM: " + nymTransactionData);
        }

        if (attribTransactionData != null && ! attribTransactionData.isCompleteForAttrib()) {
            throw new ResolutionException("Not complete for ATTRIB: " + attribTransactionData);
        }

        // DID DOCUMENT verificationMethods

        String ed25519Key = VerkeyUtil.getExpandedVerkey(did.getDidString(), nymTransactionData.getVerkey());

        List<VerificationMethod> verificationMethods = new ArrayList<>();

        VerificationMethod verificationMethodKey = VerificationMethod.builder()
                .id(URI.create(did + "#verkey"))
                .controller(URI.create(did.toString()))
                .types(Arrays.asList(DIDDOCUMENT_VERIFICATIONMETHOD_KEY_TYPES))
                .publicKeyBase58(ed25519Key)
                .build();

        verificationMethods.add(verificationMethodKey);

        // DID DOCUMENT content

        Map<String, Object> didDocumentContent;

        if (attribTransactionData != null && "diddocContent".equals(attribTransactionData.getRawKey()) && attribTransactionData.getRawValue() != null) {
            didDocumentContent = attribTransactionData.getRawValue();
        } else if (attribTransactionData != null && "endpoint".equals(attribTransactionData.getRawKey()) && attribTransactionData.getRawValue() != null && attribTransactionData.getRawValue().containsKey("endpoint")) {
            didDocumentContent = Map.of(
                    "@context", List.of("https://identity.foundation/didcomm-messaging/service-endpoint/v1"),
                    "service", List.of(
                            Map.of(
                                    "id", did + "#did-communication",
                                    "type", "did-communication",
                                    "priority", 0,
                                    "serviceEndpoint", attribTransactionData.getRawValue().get("endpoint"),
                                    "recipientKeys", List.of("#verkey"),
                                    "routingKeys", List.of()
                            )
                    ));
        } else {
            didDocumentContent = Collections.emptyMap();
        }
        if (log.isDebugEnabled()) log.debug("DID document content for " + did + ": " + didDocumentContent);

        // create DID DOCUMENT

        DIDDocument didDocument = DIDDocument.builder()
                .contexts(DIDDOCUMENT_CONTEXTS)
                .id(did.toUri())
                .verificationMethods(verificationMethods)
                .authenticationVerificationMethod(VerificationMethod.builder().id(verificationMethodKey.getId()).build())
                .build();

        for (Map.Entry<String, Object> didDocumentContentEntry : didDocumentContent.entrySet()) {
            String didDocumentContentKey = didDocumentContentEntry.getKey();
            Object didDocumentContentValue = didDocumentContentEntry.getValue();
            if (List.of("@context", "verificationMethod", "authentication").contains(didDocumentContentKey)){
                if (log.isDebugEnabled()) log.debug("Merging DID document content: " + didDocumentContentKey + " -> " + didDocumentContentValue);
                if (didDocumentContentValue instanceof List)
                    JsonLDUtils.jsonLdAddAsJsonArray(didDocument, didDocumentContentKey, (List) didDocumentContentValue);
                else
                    JsonLDUtils.jsonLdAddAsJsonArray(didDocument, didDocumentContentKey, didDocumentContentValue);
            } else {
                if (log.isDebugEnabled()) log.debug("Adding DID document content: " + didDocumentContentKey + " -> " + didDocumentContentValue);
                didDocument.getJsonObject().put(didDocumentContentKey, didDocumentContentValue);
            }
        }

        if (log.isDebugEnabled()) log.debug("Assembled DID document: " + didDocument);

        // done

        return didDocument;
    }

    public static DIDDocument assembleDeactivatedDIDDocument(DID did) {

        // create DID DOCUMENT

        DIDDocument didDocument = DIDDocument.builder()
                .contexts(DIDDOCUMENT_CONTEXTS)
                .id(did.toUri())
                .build();

        // done

        return didDocument;
    }
}
