package club.bayview.smoothieweb.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.ui.Model;

public class PageUtil {
    public static final String DEFAULT_PAGE_SIZE = "20";
    public static final String NUM_OF_ENTRIES = "numOfEntries",
            PAGE = "paramPage",
            PAGE_SIZE = "paramPageSize";

    public static Pageable createPageable(int page, int pageSize, boolean descending, String sortField, Model model) {
        Pageable pageable = PageRequest.of(page - 1, pageSize, descending ? Sort.Direction.DESC : Sort.Direction.ASC, sortField);
        model.addAttribute(PAGE, (long) page);
        model.addAttribute(PAGE_SIZE, (long) pageSize);
        return pageable;
    }
}
