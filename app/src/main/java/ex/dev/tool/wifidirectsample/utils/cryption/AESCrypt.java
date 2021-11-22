package ex.dev.tool.wifidirectsample.utils.cryption;

import java.security.GeneralSecurityException;

public class AESCrypt {

    private final static String AES_PASSWORD = "pointMobile";

    public String aesEncrypt(String data) {
        try {
            return com.scottyab.aescrypt.AESCrypt.encrypt(AES_PASSWORD, data);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String aesDecrypt(String encryptedData) {
        try {
            return com.scottyab.aescrypt.AESCrypt.decrypt(AES_PASSWORD, encryptedData);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        }
        return "";
    }
}
