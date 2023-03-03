package junit.aggregator.book;

import com.kaimono.order.service.book.Book;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.aggregator.ArgumentsAggregationException;
import org.junit.jupiter.params.aggregator.ArgumentsAggregator;

public class BookAggregator implements ArgumentsAggregator {

    @Override
    public Object aggregateArguments(ArgumentsAccessor accessor, ParameterContext context) throws ArgumentsAggregationException {
        return new Book(
                accessor.getString(0),
                accessor.getString(1),
                accessor.getString(2),
                accessor.getDouble(3)
        );
    }

}
