package uniresolver.driver.did.indy;

import com.danubetech.libindy.IndyConnection;
import com.danubetech.libindy.IndyConnectionException;
import com.danubetech.libindy.IndyConnector;
import com.danubetech.libindy.LibIndyInitializer;
import foundation.identity.did.DID;
import foundation.identity.did.DIDDocument;
import foundation.identity.did.DIDURL;
import foundation.identity.did.representations.Representations;
import org.hyperledger.indy.sdk.IndyException;
import org.hyperledger.indy.sdk.ledger.Ledger;
import org.hyperledger.indy.sdk.pool.Pool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uniresolver.DereferencingException;
import uniresolver.ResolutionException;
import uniresolver.driver.Driver;
import uniresolver.driver.did.indy.ledger.DidDocAssembler;
import uniresolver.driver.did.indy.ledger.TransactionData;
import uniresolver.result.DereferenceResult;
import uniresolver.result.ResolveResult;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DidIndyDriver implements Driver {

	private static final Logger log = LoggerFactory.getLogger(DidIndyDriver.class);

	public static final Pattern DID_INDY_PATTERN = Pattern.compile("^did:indy:(?:(\\w[-\\w]*(?::\\w[-\\w]*)*):)?([1-9A-HJ-NP-Za-km-z]{21,22})$");

	private Map<String, Object> properties;

	private LibIndyInitializer libIndyInitializer;
	private boolean openParallel;
	private IndyConnector indyConnector;

	public DidIndyDriver(LibIndyInitializer libIndyInitializer, boolean openParallel, IndyConnector indyConnector) throws IndyConnectionException{

		this.setLibIndyInitializer(libIndyInitializer);
		this.setOpenParallel(openParallel);
		this.setIndyConnector(indyConnector);

		this.initializeIndy();
	}

	public DidIndyDriver(Map<String, Object> properties) throws IndyConnectionException{

		this.setProperties(properties);

		this.initializeIndy();
	}

	public DidIndyDriver() throws IndyConnectionException {

		this(getPropertiesFromEnvironment());
	}

	private void initializeIndy() throws IndyConnectionException {

		// init libindy

		if (this.getLibIndyInitializer() != null && ! this.getLibIndyInitializer().isInitialized()) {
			this.getLibIndyInitializer().initializeLibIndy();
			if (log.isInfoEnabled()) log.info("Successfully initialized libindy.");
		}

		// open indy connections

		if (this.getIndyConnector() != null && ! this.getIndyConnector().isOpened()) {
			this.getIndyConnector().openIndyConnections(true, false, this.getOpenParallel());
			if (log.isInfoEnabled()) log.info("Successfully opened Indy connections.");
		}
	}

	private static Map<String, Object> getPropertiesFromEnvironment() {

		if (log.isDebugEnabled()) log.debug("Loading from environment: " + System.getenv());

		Map<String, Object> properties = new HashMap<> ();

		try {

			String env_libIndyPath = System.getenv("uniresolver_driver_did_indy_libIndyPath");
			String env_openParallel = System.getenv("uniresolver_driver_did_indy_openParallel");
			String env_poolConfigs = System.getenv("uniresolver_driver_did_indy_poolConfigs");
			String env_poolVersions = System.getenv("uniresolver_driver_did_indy_poolVersions");
			String env_walletNames = System.getenv("uniresolver_driver_did_indy_walletNames");
			String env_submitterDidSeeds = System.getenv("uniresolver_driver_did_indy_submitterDidSeeds");

			if (env_libIndyPath != null) properties.put("libIndyPath", env_libIndyPath);
			if (env_openParallel != null) properties.put("openParallel", env_openParallel);
			if (env_poolConfigs != null) properties.put("poolConfigs", env_poolConfigs);
			if (env_poolVersions != null) properties.put("poolVersions", env_poolVersions);
			if (env_walletNames != null) properties.put("walletNames", env_walletNames);
			if (env_submitterDidSeeds != null) properties.put("submitterDidSeeds", env_submitterDidSeeds);
		} catch (Exception ex) {

			throw new IllegalArgumentException(ex.getMessage(), ex);
		}

		return properties;
	}

	private void configureFromProperties() {

		if (log.isDebugEnabled()) log.debug("Configuring from properties: " + this.getProperties());

		try {

			String prop_libIndyPath = (String) this.getProperties().get("libIndyPath");
			String prop_openParallel = (String) this.getProperties().get("openParallel");

			this.setLibIndyInitializer(new LibIndyInitializer(
					prop_libIndyPath));

			this.setOpenParallel(Boolean.parseBoolean(prop_openParallel));

			String prop_poolConfigs = (String) this.getProperties().get("poolConfigs");
			String prop_poolVersions = (String) this.getProperties().get("poolVersions");
			String prop_walletNames = (String) this.getProperties().get("walletNames");
			String prop_submitterDidSeeds = (String) this.getProperties().get("submitterDidSeeds");

			this.setIndyConnector(new IndyConnector(
					prop_poolConfigs,
					prop_poolVersions,
					prop_walletNames,
					prop_submitterDidSeeds,
					null));
		} catch (Exception ex) {

			throw new IllegalArgumentException(ex.getMessage(), ex);
		}
	}

	@Override
	public ResolveResult resolve(DID did, Map<String, Object> resolveOptions) throws ResolutionException {

		// check if Indy connections are open

		if (! this.getIndyConnector().isOpened()) {
			throw new ResolutionException("Indy connections are not opened");
		}

		// parse identifier

		Matcher matcher = DID_INDY_PATTERN.matcher(did.getDidString());
		if (! matcher.matches()) return null;

		String network = matcher.group(1);
		String indyDid = matcher.group(2);
		if (network == null || network.trim().isEmpty()) network = "_";

		// find Indy connection

		IndyConnection indyConnection;

		try {
			indyConnection = this.getIndyConnector().getIndyConnection(network, true, true, false);
			if (indyConnection == null) {
				if (log.isInfoEnabled()) log.info("Unknown network: " + network);
				return null;
			}
		} catch (IndyConnectionException ex) {
			throw new ResolutionException("Cannot get Indy connection for network " + network + ": " + ex.getMessage(), ex);
		}

		// send GET_NYM request

		String getNymResponse;

		try {
			synchronized(indyConnection) {
				Pool.setProtocolVersion(indyConnection.getPoolVersion());
				String getNymRequest = Ledger.buildGetNymRequest(indyConnection.getSubmitterDid(), indyDid).get();
				getNymResponse = Ledger.signAndSubmitRequest(indyConnection.getPool(), indyConnection.getWallet(), indyConnection.getSubmitterDid(), getNymRequest).get();
			}
		} catch (IndyException | InterruptedException | ExecutionException ex) {
			throw new ResolutionException("Cannot send GET_NYM request: " + ex.getMessage(), ex);
		}

		if (log.isInfoEnabled()) log.info("GET_NYM for " + indyDid + ": " + getNymResponse);

		TransactionData nymTransactionData = TransactionData.fromGetNymResponse(getNymResponse);
		if (log.isDebugEnabled()) log.debug("nymTransactionData: " + nymTransactionData);

		// not found?

		if (! nymTransactionData.isFound()) {
			if (log.isInfoEnabled()) log.info("For indyDid " + indyDid + " on " + network + ": Not found. Keep watching.");
			return null;
		}

		// determine if deactivated

		boolean deactivated = nymTransactionData.getVerkey() == null;
		if (log.isDebugEnabled()) log.debug("For indyDid " + indyDid + " on " + network + ": deactivated=" + deactivated);

		// send GET_ATTR request

		String getAttrResponse;

		if (deactivated) {

			getAttrResponse = null;
		} else {

			try {
				synchronized (indyConnection) {
					Pool.setProtocolVersion(indyConnection.getPoolVersion());
					String getAttrRequest = Ledger.buildGetAttribRequest(indyConnection.getSubmitterDid(), indyDid, "diddocContent", null, null).get();
					getAttrResponse = Ledger.signAndSubmitRequest(indyConnection.getPool(), indyConnection.getWallet(), indyConnection.getSubmitterDid(), getAttrRequest).get();
				}
			} catch (IndyException | InterruptedException | ExecutionException ex) {
				throw new ResolutionException("Cannot send GET_ATTR request: " + ex.getMessage(), ex);
			}

			if (! TransactionData.hasData(getAttrResponse)) {
				try {
					synchronized (indyConnection) {
						Pool.setProtocolVersion(indyConnection.getPoolVersion());
						String getAttrRequest = Ledger.buildGetAttribRequest(indyConnection.getSubmitterDid(), indyDid, "endpoint", null, null).get();
						getAttrResponse = Ledger.signAndSubmitRequest(indyConnection.getPool(), indyConnection.getWallet(), indyConnection.getSubmitterDid(), getAttrRequest).get();
					}
				} catch (IndyException | InterruptedException | ExecutionException ex) {
					throw new ResolutionException("Cannot send GET_ATTR request: " + ex.getMessage(), ex);
				}
			}

			if (log.isInfoEnabled()) log.info("GET_ATTR for " + indyDid + ": " + getAttrResponse);
		}

		TransactionData attribTransactionData = getAttrResponse == null ? null : TransactionData.fromGetAttrResponse(getAttrResponse);
		if (log.isDebugEnabled()) log.debug("attribTransactionData: " + attribTransactionData);

		// assemble DID document

		DIDDocument didDocument;

		if (deactivated)
			didDocument = DidDocAssembler.assembleDeactivatedDIDDocument(did);
		else
			didDocument = DidDocAssembler.assembleDIDDocument(did, nymTransactionData, attribTransactionData);

		// create DID DOCUMENT METADATA

		Map<String, Object> didDocumentMetadata = new LinkedHashMap<> ();
		if (deactivated) didDocumentMetadata.put("deactivated", deactivated);
		didDocumentMetadata.put("network", indyConnection.getPoolConfigName());
		didDocumentMetadata.put("poolVersion", indyConnection.getPoolVersion());
		didDocumentMetadata.put("submitterDid", indyConnection.getSubmitterDid());
		if (nymTransactionData != null) didDocumentMetadata.put("nymResponse", nymTransactionData.getResponseMap());
		if (attribTransactionData != null) didDocumentMetadata.put("attribResponse", attribTransactionData.getResponseMap());

		// create RESOLVE RESULT

		ResolveResult resolveResult = ResolveResult.build(null, didDocument, didDocumentMetadata);
		resolveResult.setContentType(Representations.DEFAULT_MEDIA_TYPE);

		// done

		return resolveResult;
	}

	@Override
	public DereferenceResult dereference(DIDURL didurl, Map<String, Object> map) throws DereferencingException, ResolutionException {
		throw new RuntimeException("Not implemented");
	}

	@Override
	public Map<String, Object> properties() {
		return this.getProperties();
	}

	/*
	 * Getters and setters
	 */

	public Map<String, Object> getProperties() {
		return this.properties;
	}

	public void setProperties(Map<String, Object> properties) {
		this.properties = properties;
		this.configureFromProperties();
	}

	public LibIndyInitializer getLibIndyInitializer() {
		return libIndyInitializer;
	}

	public void setLibIndyInitializer(LibIndyInitializer libIndyInitializer) {
		this.libIndyInitializer = libIndyInitializer;
	}

	public boolean getOpenParallel() {
		return openParallel;
	}

	public void setOpenParallel(boolean openParallel) {
		this.openParallel = openParallel;
	}

	public IndyConnector getIndyConnector() {
		return indyConnector;
	}

	public void setIndyConnector(IndyConnector indyConnector) {
		this.indyConnector = indyConnector;
	}
}
