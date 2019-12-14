package club.bayview.smoothieweb.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@ToString
public class TestData implements Serializable {

    List<List<Problem.ProblemBatchCase>> testData;

}
