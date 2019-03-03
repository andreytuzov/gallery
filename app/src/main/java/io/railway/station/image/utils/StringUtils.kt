package io.railway.station.image.utils


/**
 * Get short name
 */
fun String.limitWithLastConsonant(limit: Int) =
        if (length <= limit) this
        else {
            val pattern = "аоеёиуэюя"
            var index = limit - 1
            // Skip the vowels from pattern and find first consonant
            while (pattern.indexOf(get(index)) != -1) index--
            substring(0, index + 1)
        }


fun String?.ifNullOrEmpty(defValue: String) = if (isNullOrEmpty()) defValue else this