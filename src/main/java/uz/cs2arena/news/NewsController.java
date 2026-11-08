package uz.cs2arena.news;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import uz.cs2arena.common.ApiException;
import uz.cs2arena.common.PageDto;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsRepository news;

    public NewsController(NewsRepository news) {
        this.news = news;
    }

    @GetMapping
    public PageDto<NewsDto> list(@RequestParam(defaultValue = "0") int page) {
        return PageDto.of(news.findByPublishedTrueOrderByCreatedAtDesc(PageRequest.of(Math.max(0, page), 10)),
                NewsDto::of);
    }

    @GetMapping("/{id}")
    public NewsDto get(@PathVariable Long id) {
        return news.findById(id).filter(News::isPublished).map(NewsDto::of)
                .orElseThrow(() -> ApiException.notFound("Yangilik topilmadi"));
    }
}
