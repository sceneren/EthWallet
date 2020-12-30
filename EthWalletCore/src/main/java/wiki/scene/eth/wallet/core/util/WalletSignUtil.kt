package wiki.scene.eth.wallet.core.util

import org.bouncycastle.util.encoders.Hex
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Hash
import org.web3j.crypto.Sign
import java.math.BigInteger

object WalletSignUtil {
    fun sign(data: String, privateKey: String): String {
        try {
            val realPrivateKey = if (privateKey.startsWith("0x")) {
                privateKey.substring(2)
            } else {
                privateKey
            }
            val realPrivateKeyInt = BigInteger(realPrivateKey, 16)
            val pubKey = Sign.publicKeyFromPrivate(realPrivateKeyInt)
            val keyPair = ECKeyPair(realPrivateKeyInt, pubKey)
            val msgHash = Hash.sha3(data.toByteArray())
            val signature = Sign.signMessage(msgHash, keyPair, false)
            return Hex.toHexString(signature.v) + Hex.toHexString(signature.r) + Hex.toHexString(signature.s) + pubKey.toString(16)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }

    }
}