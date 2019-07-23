import java.nio.file.{Files, Paths, StandardOpenOption}
import java.security._
import java.util.Base64

val keyGen = KeyPairGenerator.getInstance("EC")
val random = SecureRandom.getInstance("SHA1PRNG")
keyGen.initialize(256, random)
val pair = keyGen.generateKeyPair()
val priv = pair.getPrivate
val publ = pair.getPublic
val encoder = Base64.getEncoder
val privStr = "-----BEGIN PRIVATE KEY-----\n" +
  encoder.encodeToString(priv.getEncoded) +
  "\n-----END PRIVATE KEY-----\n"
val publStr = "-----BEGIN PUBLIC KEY-----\n" +
  encoder.encodeToString(publ.getEncoded) +
  "\n-----END PUBLIC KEY-----\n"
println(privStr)
println(publStr)
val privKeyPath = Paths.get("src", "test", "resources", "auth.key")
val publKeyPath = Paths.get("src", "test", "resources", "auth.key.pub")
Files.deleteIfExists(privKeyPath)
Files.deleteIfExists(publKeyPath)
Files.writeString(privKeyPath, privStr, StandardOpenOption.CREATE_NEW)
Files.writeString(publKeyPath, publStr, StandardOpenOption.CREATE_NEW)
