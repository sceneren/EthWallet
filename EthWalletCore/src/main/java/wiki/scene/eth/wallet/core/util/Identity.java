package wiki.scene.eth.wallet.core.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Utils;
import org.bitcoinj.core.VarInt;
import org.consenlabs.tokencore.foundation.crypto.AES;
import org.consenlabs.tokencore.foundation.crypto.Crypto;
import org.consenlabs.tokencore.foundation.crypto.Hash;
import org.consenlabs.tokencore.foundation.crypto.Multihash;
import org.consenlabs.tokencore.foundation.utils.ByteUtil;
import org.consenlabs.tokencore.foundation.utils.MnemonicUtil;
import org.consenlabs.tokencore.foundation.utils.NumericUtil;
import org.consenlabs.tokencore.wallet.keystore.EOSKeystore;
import org.consenlabs.tokencore.wallet.keystore.HDMnemonicKeystore;
import org.consenlabs.tokencore.wallet.keystore.IMTKeystore;
import org.consenlabs.tokencore.wallet.keystore.IdentityKeystore;
import org.consenlabs.tokencore.wallet.keystore.V3MnemonicKeystore;
import org.consenlabs.tokencore.wallet.model.BIP44Util;
import org.consenlabs.tokencore.wallet.model.ChainType;
import org.consenlabs.tokencore.wallet.model.Messages;
import org.consenlabs.tokencore.wallet.model.Metadata;
import org.consenlabs.tokencore.wallet.model.TokenException;
import org.consenlabs.tokencore.wallet.transaction.EthereumSign;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import wiki.scene.eth.wallet.core.config.WalletType;

/**
 * Created by xyz on 2017/12/11.
 */

public class Identity {

    public static Identity currentEthIdentity;
    public static Identity currentSetIdentity;
    private static final String ETH_FILE_NAME = "eth_identity.json";
    private static final String SET_FILE_NAME = "set_identity.json";

    private IdentityKeystore ethKeystore;
    private IdentityKeystore setKeystore;
    private final List<Wallet> ethWallets = new ArrayList<>();
    private final List<Wallet> setWallets = new ArrayList<>();

    public String getIdentifier(WalletType walletType) {
        if (walletType == WalletType.ETH_WALLET_TYPE_ETH) {
            return this.ethKeystore.getIdentifier();
        } else {
            return this.setKeystore.getIdentifier();
        }
    }

    public String getIpfsId(WalletType walletType) {
        if (walletType == WalletType.ETH_WALLET_TYPE_ETH) {
            return this.ethKeystore.getIpfsId();
        } else {
            return this.setKeystore.getIpfsId();
        }
    }

    public List<Wallet> getWallets(WalletType walletType) {
        if (walletType == WalletType.ETH_WALLET_TYPE_ETH) {
            return this.ethWallets;
        } else {
            return this.setWallets;
        }
    }

    public Metadata getMetadata(WalletType walletType) {
        if (walletType == WalletType.ETH_WALLET_TYPE_ETH) {
            return ethKeystore.getMetadata();
        } else {
            return setKeystore.getMetadata();
        }
    }

    public static Identity getCurrentIdentity(WalletType walletType) {
        synchronized (Identity.class) {
            if (walletType == WalletType.ETH_WALLET_TYPE_ETH) {
                if (currentEthIdentity == null) {
                    currentEthIdentity = tryLoadFromFile(walletType);
                }
                return currentEthIdentity;
            } else {
                if (currentSetIdentity == null) {
                    currentSetIdentity = tryLoadFromFile(walletType);
                }
                return currentSetIdentity;
            }

        }
    }

    private Identity(WalletType walletType, IdentityKeystore keystore) {
        if (walletType == WalletType.ETH_WALLET_TYPE_ETH) {
            this.ethKeystore = keystore;
            for (String walletId : this.ethKeystore.getWalletIDs()) {
                ethWallets.add(WalletManager.findWalletById(walletId));
            }
        } else {
            this.setKeystore = keystore;
            for (String walletId : this.setKeystore.getWalletIDs()) {
                setWallets.add(WalletManager.findWalletById(walletId));
            }
        }

    }

    private Identity(WalletType walletType, Metadata metadata, List<String> mnemonicCodes, String password) {
        if (walletType == WalletType.ETH_WALLET_TYPE_ETH) {
            this.ethKeystore = new IdentityKeystore(metadata, mnemonicCodes, password);
            currentEthIdentity = this;
        } else {
            this.setKeystore = new IdentityKeystore(metadata, mnemonicCodes, password);
            currentSetIdentity = this;
        }

    }


    public static Identity createIdentity(WalletType walletType, String name, String password, String passwordHit, String network, String segWit) {
        List<String> mnemonicCodes = MnemonicUtil.randomMnemonicCodes();
        Metadata metadata = new Metadata();
        metadata.setName(name);
        metadata.setPasswordHint(passwordHit);
        metadata.setSource(Metadata.FROM_NEW_IDENTITY);
        metadata.setNetwork(network);
        metadata.setSegWit(segWit);
        Identity identity = new Identity(walletType, metadata, mnemonicCodes, password);
        if (walletType == WalletType.ETH_WALLET_TYPE_ETH) {
            currentEthIdentity = identity;
        } else {
            currentSetIdentity = identity;
        }
        return identity;
    }

    public static Identity recoverIdentity(WalletType walletType, String mnemonic, String name, String password,
                                           String passwordHit, String network, String segWit) {
        List<String> mnemonicCodes = Arrays.asList(mnemonic.split(" "));
        Metadata metadata = new Metadata();
        metadata.setName(name);
        metadata.setPasswordHint(passwordHit);
        metadata.setSource(Metadata.FROM_RECOVERED_IDENTITY);
        metadata.setNetwork(network);
        metadata.setSegWit(segWit);
        Identity identity = new Identity(walletType, metadata, mnemonicCodes, password);
        if (walletType == WalletType.ETH_WALLET_TYPE_ETH) {
            currentEthIdentity = identity;
        } else {
            currentSetIdentity = identity;
        }
        return identity;
    }

    public void deleteIdentity(WalletType walletType, String password) {
        if (walletType == WalletType.ETH_WALLET_TYPE_ETH) {
            if (!this.ethKeystore.verifyPassword(password)) {
                throw new TokenException(Messages.WALLET_INVALID_PASSWORD);
            }

            if (WalletManager.cleanKeystoreDirectory()) {
                WalletManager.clearKeystoreMap();
                currentEthIdentity = null;
            }
        } else {
            if (!this.setKeystore.verifyPassword(password)) {
                throw new TokenException(Messages.WALLET_INVALID_PASSWORD);
            }

            if (WalletManager.cleanKeystoreDirectory()) {
                WalletManager.clearKeystoreMap();
                currentSetIdentity = null;
            }
        }

    }

    public String exportIdentity(WalletType walletType, String password) {
        if (walletType == WalletType.ETH_WALLET_TYPE_ETH) {
            return this.ethKeystore.decryptMnemonic(password);
        } else {
            return this.setKeystore.decryptMnemonic(password);
        }
    }

    public void addWallet(WalletType walletType, Wallet wallet) {
        if (walletType == WalletType.ETH_WALLET_TYPE_ETH) {
            this.ethKeystore.getWalletIDs().add(wallet.getId());
            this.ethWallets.add(wallet);
        } else {
            this.setKeystore.getWalletIDs().add(wallet.getId());
            this.setWallets.add(wallet);
        }
        flush(walletType);
    }

    void removeWallet(WalletType walletType, String walletId) {
        if (walletType == WalletType.ETH_WALLET_TYPE_ETH) {
            this.ethKeystore.getWalletIDs().remove(walletId);

            int idx = 0;
            for (; idx < ethWallets.size(); idx++) {
                if (ethWallets.get(idx).getId().equals(walletId)) {
                    break;
                }
            }
            this.ethWallets.remove(idx);
        } else {
            this.setKeystore.getWalletIDs().remove(walletId);

            int idx = 0;
            for (; idx < setWallets.size(); idx++) {
                if (setWallets.get(idx).getId().equals(walletId)) {
                    break;
                }
            }
            this.setWallets.remove(idx);
        }
        flush(walletType);
    }

    public List<Wallet> deriveWallets(WalletType walletType, List<String> chainTypes, String password) {
        List<Wallet> wallets = new ArrayList<>();
        String mnemonic = exportIdentity(walletType, password);
        List<String> mnemonics = Arrays.asList(mnemonic.split(" "));
        for (String chainType : chainTypes) {
            Wallet wallet;
            switch (chainType) {
                case ChainType.BITCOIN:
                    wallet = deriveBitcoinWallet(walletType, mnemonics, password, this.getMetadata(walletType).getSegWit());
                    break;
                case ChainType.ETHEREUM:
                    wallet = deriveEthereumWallet(walletType, mnemonics, password);
                    break;
                case ChainType.EOS:
                    wallet = deriveEOSWallet(walletType, mnemonics, password);
                    break;
                default:
                    throw new TokenException(String.format("Doesn't support deriving %s wallet", chainType));
            }
            addWallet(walletType, wallet);
            wallets.add(wallet);
        }

        return wallets;
    }


    private static Identity tryLoadFromFile(WalletType walletType) {
        try {
            File file;
            if (walletType == WalletType.ETH_WALLET_TYPE_ETH) {
                file = new File(WalletManager.getDefaultKeyDirectory(), ETH_FILE_NAME);
            } else {
                file = new File(WalletManager.getDefaultKeyDirectory(), SET_FILE_NAME);
            }
            ObjectMapper mapper = new ObjectMapper()
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
            IdentityKeystore keystore = mapper.readValue(file, IdentityKeystore.class);
            return new Identity(walletType, keystore);
        } catch (IOException ignored) {
            return null;
        }
    }

    private void flush(WalletType walletType) {
        try {
            if (walletType == WalletType.ETH_WALLET_TYPE_ETH) {
                File file = new File(WalletManager.getDefaultKeyDirectory(), ETH_FILE_NAME);
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                objectMapper.writeValue(file, this.ethKeystore);
            } else {
                File file = new File(WalletManager.getDefaultKeyDirectory(), SET_FILE_NAME);
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                objectMapper.writeValue(file, this.setKeystore);
            }

        } catch (IOException ex) {
            throw new TokenException(Messages.WALLET_STORE_FAIL, ex);
        }
    }

    private Wallet deriveBitcoinWallet(WalletType walletType, List<String> mnemonicCodes, String password, String segWit) {
        Metadata walletMetadata = new Metadata();
        walletMetadata.setChainType(ChainType.BITCOIN);
        walletMetadata.setPasswordHint(this.getMetadata(walletType).getPasswordHint());
        walletMetadata.setSource(this.getMetadata(walletType).getSource());
        walletMetadata.setNetwork(this.getMetadata(walletType).getNetwork());
        walletMetadata.setName("BTC");
        walletMetadata.setSegWit(segWit);
        String path;
        if (Metadata.P2WPKH.equals(segWit)) {
            path = this.getMetadata(walletType).isMainNet() ? BIP44Util.BITCOIN_SEGWIT_MAIN_PATH : BIP44Util.BITCOIN_SEGWIT_TESTNET_PATH;
        } else {
            path = this.getMetadata(walletType).isMainNet() ? BIP44Util.BITCOIN_MAINNET_PATH : BIP44Util.BITCOIN_TESTNET_PATH;
        }

        IMTKeystore keystore = HDMnemonicKeystore.create(walletMetadata, password, mnemonicCodes, path);
        return WalletManager.createWallet(keystore);
    }

    private Wallet deriveEthereumWallet(WalletType walletType, List<String> mnemonics, String password) {
        Metadata walletMetadata = new Metadata();
        walletMetadata.setChainType(ChainType.ETHEREUM);
        walletMetadata.setPasswordHint(this.getMetadata(walletType).getPasswordHint());
        walletMetadata.setSource(this.getMetadata(walletType).getSource());
        walletMetadata.setName(walletType == WalletType.ETH_WALLET_TYPE_ETH ? "ETH" : "SET");
        IMTKeystore keystore = V3MnemonicKeystore.create(walletMetadata, password, mnemonics, BIP44Util.ETHEREUM_PATH);
        return WalletManager.createWallet(keystore);
    }

    private Wallet deriveEOSWallet(WalletType walletType, List<String> mnemonics, String password) {
        Metadata metadata = new Metadata();
        metadata.setChainType(ChainType.EOS);
        metadata.setPasswordHint(this.getMetadata(walletType).getPasswordHint());
        metadata.setSource(this.getMetadata(walletType).getSource());
        metadata.setName("EOS");
        IMTKeystore keystore = EOSKeystore.create(metadata, password, "", mnemonics, BIP44Util.EOS_LEDGER, null);
        return WalletManager.createWallet(keystore);
    }

    public String encryptDataToIPFS(String originData) {
        long unixTimestamp = Utils.currentTimeSeconds();
        byte[] iv = NumericUtil.generateRandomBytes(16);
        return encryptDataToIPFS(originData, unixTimestamp, iv);
    }

    String encryptDataToIPFS(String originData, long unixtime, byte[] iv) {
        int headerLength = 21;
        byte[] toSign = new byte[headerLength + 32];
        byte version = 0x03;
        toSign[0] = version;
        byte[] timestamp = new byte[4];

        Utils.uint32ToByteArrayLE(unixtime, timestamp, 0);
        System.arraycopy(timestamp, 0, toSign, 1, 4);
        byte[] encryptionKey = NumericUtil.hexToBytes(this.ethKeystore.getEncKey());

        System.arraycopy(iv, 0, toSign, 5, 16);

        byte[] encKey = Arrays.copyOf(encryptionKey, 16);
        byte[] ciphertext = AES.encryptByCBC(originData.getBytes(Charset.forName("UTF-8")), encKey, iv);
        VarInt ciphertextLength = new VarInt(ciphertext.length);

        System.arraycopy(Hash.merkleHash(ciphertext), 0, toSign, headerLength, 32);
        String signature = EthereumSign.sign(NumericUtil.bytesToHex(toSign), encryptionKey);
        byte[] signatureBytes = NumericUtil.hexToBytes(signature);
        int totalLen = (int) (headerLength + ciphertextLength.getSizeInBytes() + ciphertextLength.value + 65);
        byte[] payload = new byte[totalLen];
        int destPos = 0;
        System.arraycopy(toSign, 0, payload, destPos, headerLength);
        destPos += headerLength;
        System.arraycopy(ciphertextLength.encode(), 0, payload, destPos, ciphertextLength.getSizeInBytes());
        destPos += ciphertextLength.getSizeInBytes();
        System.arraycopy(ciphertext, 0, payload, destPos, (int) ciphertextLength.value);
        destPos += (int) ciphertextLength.value;

        System.arraycopy(signatureBytes, 0, payload, destPos, 65);
        return NumericUtil.bytesToHex(payload);

    }

    public String decryptDataFromIPFS(String encryptedData) {
        int headerLength = 21;

        byte[] payload = NumericUtil.hexToBytes(encryptedData);

        byte version = payload[0];
        if (version != 0x03) {
            throw new TokenException(Messages.UNSUPPORT_ENCRYPTION_DATA_VERSION);
        }
        int srcPos = 1;
        byte[] toSign = new byte[headerLength + 32];
        System.arraycopy(payload, 0, toSign, 0, headerLength);

        byte[] timestamp = new byte[4];
        System.arraycopy(payload, srcPos, timestamp, 0, 4);
        srcPos += 4;

        byte[] encryptionKey = NumericUtil.hexToBytes(this.ethKeystore.getEncKey());
        byte[] iv = new byte[16];
        System.arraycopy(payload, srcPos, iv, 0, 16);
        srcPos += 16;
        VarInt ciphertextLength = new VarInt(payload, srcPos);
        srcPos += ciphertextLength.getSizeInBytes();
        byte[] ciphertext = new byte[(int) ciphertextLength.value];
        System.arraycopy(payload, srcPos, ciphertext, 0, (int) ciphertextLength.value);
        System.arraycopy(Hash.merkleHash(ciphertext), 0, toSign, headerLength, 32);
        srcPos += ciphertextLength.value;
        byte[] encKey = Arrays.copyOf(encryptionKey, 16);
        String content = new String(AES.decryptByCBC(ciphertext, encKey, iv), Charset.forName("UTF-8"));

        byte[] signature = new byte[65];
        System.arraycopy(payload, srcPos, signature, 0, 65);
        try {
            BigInteger pubKey = EthereumSign.ecRecover(NumericUtil.bytesToHex(toSign), NumericUtil.bytesToHex(signature));
            ECKey ecKey = ECKey.fromPublicOnly(ByteUtil.concat(new byte[]{0x04}, NumericUtil.bigIntegerToBytesWithZeroPadded(pubKey, 64)));
            String recoverIpfsID = new Multihash(Multihash.Type.sha2_256, Hash.sha256(ecKey.getPubKey())).toBase58();

            if (!this.ethKeystore.getIpfsId().equals(recoverIpfsID)) {
                throw new TokenException(Messages.INVALID_ENCRYPTION_DATA_SIGNATURE);
            }

        } catch (SignatureException e) {
            throw new TokenException(Messages.INVALID_ENCRYPTION_DATA_SIGNATURE);
        }
        return content;
    }


    public String signAuthenticationMessage(WalletType walletType, int accessTime, String deviceToken, String password) {
        Crypto crypto = this.ethKeystore.getCrypto();
        byte[] decrypted = crypto.decryptEncPair(password, this.ethKeystore.getEncAuthKey());
        String data = String.format(Locale.ENGLISH, "%d.%s.%s", accessTime, getIdentifier(walletType), deviceToken);
        return EthereumSign.sign(data, decrypted);
    }
}
