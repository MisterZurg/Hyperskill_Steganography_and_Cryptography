package cryptography

import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.awt.Color
import java.io.File
import java.io.IOException

fun main() {
    do { // Repeat "cycle" until "exit" action
        println("Task (hide, show, exit):")
        val action: String = readLine()!!
        when (action) {
            "hide" -> hideImageDriver()         // println("Hiding message in image.")
            "show" -> showHiddenMessageDriver() // println("Obtaining message from image.")
            "exit" -> {
                println("Bye!")
                return
            }
            else -> {
                println("Wrong task: ${action}")
            }
        }
    } while (!action.equals("exit"))
}

fun hideImageDriver() {
    println("Input image file:")
    val inputImageFileName = readLine()!!
    lateinit var bufferedImage : BufferedImage
    try {
        bufferedImage = ImageIO.read(File(inputImageFileName)) // throw FileNotFoundException("")
    } catch (e: Exception) {
        println("Can't read input file!")
        main()
    }

    println("Output image file:")
    val outputImageFileName = readLine()!!

    println("Message to hide:")
    // Adding EndOFSequence symbols to the message
    // Stage 3
    //val message = (readLine()!! + "\u0000\u0000\u0003").encodeToByteArray()
    val message = readLine()!!

    println("Password:")
    val password = readLine()!!
    // We just add encryptMethod
    val encryptedMessage = (messageEncrypt(message , password) + "\u0000\u0000\u0003").encodeToByteArray()

    // println("Input Image: ${inputImageFileName}")
    // println("Output Image: ${outputImageFileName}")
    // Check message length
    // if (bufferedImage.width * bufferedImage.height < message.size * 8) {
    if (bufferedImage.width * bufferedImage.height < encryptedMessage.size * 8) {
        println("The input image is not large enough to hold this message.")
        return
        // main()
    }
    // Setting message bits into blue bit
    var messageBits = ""
    // message.forEach { messageBits += it.to8bits(2) }
    encryptedMessage.forEach { messageBits += it.to8bits(2) }
    for (y in 0 until bufferedImage.height) {
        for (x in 0 until bufferedImage.width) {
            val color = Color(bufferedImage.getRGB(x, y))
            val pixelNumber = x + y * bufferedImage.height
            if (pixelNumber < messageBits.length) {
                val bit = messageBits[pixelNumber].toInt() - 48 // Char 0 = 48, Char 1 = 49
                bufferedImage.setRGB(x, y, setBlueBit(color, bit))
            } else {
                bufferedImage.setRGB(x, y, color.rgb)
            }
        }
    }

    lateinit var outputImageFile : File
    try {
        outputImageFile = File(outputImageFileName)
        ImageIO.write(bufferedImage, "png", outputImageFile)
    }
    catch(e: IOException) {
        println("Can't write to output file!")
        return
        // main()
    }
    println("Message saved in ${outputImageFile} image.")
}

// Stage 4/4 Encrypt the message
fun messageEncrypt(message : String, password: String): String{
    var remPassword = message.length % password.length
    var encSequence = password.repeat(message.length / password.length) + password.substring(0..remPassword)
    return message xor encSequence
}

infix fun String.xor(that: String) = mapIndexed { index, c ->
    that[index].toInt().xor(c.toInt())
}.joinToString(separator = "") {
    it.toChar().toString()
}

fun Byte.to8bits(radix: Int): String {
    val s = this.toString(radix)
    var append = ""
    for (i in 1..8 - s.length)
        append += "0"
    return append + s
}

fun setBlueBit(color: Color, bit: Int): Int {
    // Apply bit mask to a color to set least significant bit
    var newColor = 0
    if (color.blue % 2 == 0) {
        newColor = Color(color.red, color.green, color.blue.or(bit)).rgb
    } else {
        newColor = Color(color.red, color.green, color.blue.and(254 + bit)).rgb
    }
    return newColor
}

fun showHiddenMessageDriver() {
    val stringEOS = "000000000000000000000011"
    println("Input image file:")
    val inputImageFileName = readLine()!!
    lateinit var bufferedImage: BufferedImage
    try {
        bufferedImage = ImageIO.read(File(inputImageFileName))
    } catch (e: IOException) {
        println("Can't read input file!")
        return
    }
    // Stage 4 Addition
    println("Password:")
    val password = readLine()!!

    val messageWidth = bufferedImage.width
    val messageHeight = bufferedImage.height

    var messageBits = ""

    outLoop@ for (y in 0 until messageHeight) {
        for (x in 0 until messageWidth) {
            val c = Color(bufferedImage.getRGB(x, y)).blue
            val bit = c.and(1)

            messageBits += bit.toString()
            val s = messageBits.length
            if (s >= 32 && messageBits.drop(s - 24) == stringEOS)
                break@outLoop
        }
    }
    val messageBytes = messageBits.dropLast(24).chunked(8)
    val arr = ByteArray(messageBytes.size)
    for (i in messageBytes.indices) {
        arr[i] = messageBytes[i].toByte(2)
    }
    arr.toString(Charsets.UTF_8)
    val message = arr.toString(Charsets.UTF_8)
    // println("Message:\n${message}")
    println("Message:\n${messageEncrypt(message, password)}")

}

// Appendix from Stage 2
fun hideImageHandler(inputImage: BufferedImage): BufferedImage {

    for (i in 0 until inputImage.width) {
        for (j in 0 until inputImage.height) {
            // The pixel value can be received using the following syntax
            val color = Color(inputImage.getRGB(i, j))

            val rgb = Color(
                setLeastSignificantBitToOne(color.red),
                setLeastSignificantBitToOne(color.green),
                setLeastSignificantBitToOne(color.blue)
            ).rgb

            inputImage.setRGB(i, j, rgb)
        }
    }
    return inputImage
}

fun setLeastSignificantBitToOne(pixel: Int): Int {
    return if (pixel % 2 == 0) pixel + 1 else pixel
}