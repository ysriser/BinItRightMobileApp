package sg.edu.nus.bin

data class CheckInData(
    val userId: Int,
    val recordedAt: String,
    val duration: Int,
    val binId: Int,
    val itemId: Int,
    val quantity: Int
)
