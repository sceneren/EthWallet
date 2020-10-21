package wiki.scene.eth.wallet.core.util

import io.github.novacrypto.bip32.ExtendedPrivateKey
import io.github.novacrypto.bip32.networks.Bitcoin
import io.github.novacrypto.bip39.MnemonicGenerator
import io.github.novacrypto.bip39.SeedCalculator
import io.github.novacrypto.bip39.Words
import io.github.novacrypto.bip39.wordlists.English
import io.github.novacrypto.bip44.AddressIndex
import io.github.novacrypto.bip44.BIP44
import io.reactivex.Observable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.crypto.Wallet
import org.web3j.crypto.WalletFile
import wiki.scene.eth.wallet.core.ext.changeIOThread
import wiki.scene.eth.wallet.core.ext.changeNewThread
import java.security.SecureRandom
import java.security.Security


object EthWalletUtils {
    fun init() {
        setupBouncyCastle()
    }

    private fun setupBouncyCastle() {
        val provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)
                ?: // Web3j will set up the provider lazily when it's first used.
                return
        if (provider.javaClass == BouncyCastleProvider::class.java) {
            // BC with same package name, shouldn't happen in real life.
            return
        }
        // Android registers its own BC provider. As it might be outdated and might not include
        // all needed ciphers, we substitute it with a known BC bundled in the app.
        // Android's BC has its package rewritten to "com.android.org.bouncycastle" and because
        // of that it's possible to have another BC implementation loaded in VM.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }

    fun createLightWallet(password: String): Observable<WalletFile> {
        return Observable.create<WalletFile> {
            it.onNext(Wallet.createLight(password, Keys.createEcKeyPair()))
        }.changeIOThread()
    }

    /**
     * 创建助记词
     * @return
     */
    fun createMnemonicList(): Observable<List<String>> {
        return Observable.create<List<String>> {
            val sb = StringBuilder()
            val entropy = ByteArray(Words.TWELVE.byteLength())
            SecureRandom().nextBytes(entropy)
            MnemonicGenerator(English.INSTANCE).createMnemonic(entropy, sb::append)
            val mnemonicStr = sb.toString()
            val result = mnemonicStr.split("").toList()
            if (result.size == Words.TWELVE.byteLength()) {
                it.onNext(result)
            } else {
                it.onError(Throwable())
            }
        }.changeNewThread()
    }

    fun createMnemonic(): Observable<String> {
        return Observable.create<String> {
            val sb = StringBuilder()
            val entropy = ByteArray(Words.TWELVE.byteLength())
            SecureRandom().nextBytes(entropy)
            MnemonicGenerator(English.INSTANCE).createMnemonic(entropy, sb::append)
            val mnemonicStr = sb.toString()
            val result = mnemonicStr.split("").toList()
            if (result.size == Words.TWELVE.byteLength()) {
                it.onNext(mnemonicStr)
            } else {
                it.onError(Throwable())
            }
        }.changeNewThread()
    }

    /**
     * 创建KeyPair
     */


    /**
     * 根据助记词推导钥匙对
     */
    fun createKeyPair(mnemonic: String): Observable<ECKeyPair> {
        return Observable.create<ECKeyPair> {
            val addressIndex = BIP44.m()
                    .purpose44()
                    .coinType(60)
                    .account(0)
                    .external()
                    .address(0)
            // 2.从助记符计算seed，然后获得主/根密钥；注意，为通用设置bip39的密码为""
            val seed = SeedCalculator().calculateSeed(mnemonic, "")
            val rootKey = ExtendedPrivateKey.fromSeed(seed, Bitcoin.MAIN_NET)
            val extendedBase58 = rootKey.extendedBase58()
            // 3. 获取从主/根密钥派生的子私钥
            val childPrivateKey = rootKey.derive(addressIndex, AddressIndex.DERIVATION)
            val childExtendedBase58 = childPrivateKey.extendedBase58()
            // 4.获取密钥对
            val privateKeyBytes: ByteArray = childPrivateKey.extendedKeyByteArray()
            val keyPair = ECKeyPair.create(privateKeyBytes)
            //获取需要的内容
//            val privateKey: String = childPrivateKey.neuter().p2pkhAddress()
//            val publicKey: String = childPrivateKey.neuter().getPublicKey()
//            val address = Keys.getAddress(keyPair)
            it.onNext(keyPair)
        }.changeIOThread()
    }

}