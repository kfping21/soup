import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zjgsu.soup.GameData
import com.zjgsu.soup.R

class QuestionPagerAdapter(
    private val questions: List<GameData>,
    private val onItemClick: (GameData) -> Unit
) : RecyclerView.Adapter<QuestionPagerAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.questionTitle)
        val preview: TextView = view.findViewById(R.id.questionPreview)
        val difficulty: TextView = view.findViewById(R.id.difficultyBadge)
        val tags: TextView = view.findViewById(R.id.tagsText)
        val rating: TextView = view.findViewById(R.id.ratingText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_question_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val question = questions[position]

        holder.title.text = question.title
        holder.preview.text = question.description.take(50) + "..."
        holder.difficulty.text = question.difficulty
        holder.tags.text = "#${question.difficulty} #${question.tags?.joinToString(" #")}"
        holder.rating.text = "4.7分" // 示例数据

        holder.itemView.setOnClickListener { onItemClick(question) }
    }

    override fun getItemCount() = questions.size
}