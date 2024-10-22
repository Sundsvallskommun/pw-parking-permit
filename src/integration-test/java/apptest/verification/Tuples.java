package apptest.verification;

import org.assertj.core.groups.Tuple;

import java.util.ArrayList;
import java.util.List;

public class Tuples extends ArrayList<Tuple> {

    public static Tuples create() {
        return new Tuples();
    }

    public Tuples with(Tuple t) {
        super.add(t);
        return this;
    }

    public Tuples with(List<Tuple> l) {
        super.addAll(l);
        return this;
    }
}