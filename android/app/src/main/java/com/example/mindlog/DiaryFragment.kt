package com.example.mindlog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DiaryFragment : Fragment() {

    // 1. RecyclerView와 Adapter를 담을 변수 선언
    private lateinit var recyclerView: RecyclerView
    private lateinit var journalAdapter: JournalAdapter // 아래에서 만들 어댑터

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // fragment_diary.xml을 화면으로 만듭니다.
        val view = inflater.inflate(R.layout.fragment_diary, container, false)
        return view
    }

    // 2. onCreateView가 끝난 후, 만들어진 View에 접근하여 초기화 작업을 합니다.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView를 XML 레이아웃에서 찾아옵니다.
        recyclerView = view.findViewById(R.id.rv_diary_feed)

        // RecyclerView가 아이템을 어떻게 배치할지 결정합니다. (여기서는 세로 리스트)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // 3. 가짜 데이터 생성
        val dummyData = listOf("여행", "공부", "운동", "휴식", "친구와의 만남")

        // 4. 어댑터를 생성하고 RecyclerView에 연결합니다.
        journalAdapter = JournalAdapter(dummyData)
        recyclerView.adapter = journalAdapter
    }
}


// --- 아래에 간단한 RecyclerView 어댑터를 추가합니다. ---

// 5. RecyclerView.Adapter 클래스 정의
class JournalAdapter(private val items: List<String>) : RecyclerView.Adapter<JournalAdapter.JournalViewHolder>() {

    // 아이템 하나의 모양(View)을 담는 ViewHolder
    class JournalViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // item_journal_card.xml 안의 뷰들을 여기에 연결할 수 있습니다.
        // 예: val title: TextView = view.findViewById(R.id.journal_title)
    }

    // ViewHolder를 생성하고, 어떤 레이아웃을 사용할지 결정합니다. (item_journal_card.xml 사용)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_journal_card, parent, false)
        return JournalViewHolder(view)
    }

    // 각 위치(position)에 해당하는 데이터를 ViewHolder에 채워 넣습니다.
    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        val item = items[position]
        // 예: holder.title.text = item
        // 지금은 데이터를 채우는 코드가 없어도 아이템 레이아웃 자체는 화면에 나타납니다.
    }

    // 전체 아이템의 개수를 반환합니다.
    override fun getItemCount(): Int {
        return items.size
    }
}
