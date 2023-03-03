package junit.aggregator.order;

import com.kaimono.order.service.order.domain.Order;
import com.kaimono.order.service.order.domain.OrderStatus;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.aggregator.ArgumentsAggregationException;
import org.junit.jupiter.params.aggregator.ArgumentsAggregator;

public class OrderAggregator implements ArgumentsAggregator {

    @Override
    public Object aggregateArguments(ArgumentsAccessor accessor, ParameterContext context) throws ArgumentsAggregationException {
        return Order.of(
                accessor.getString(0),
                accessor.getString(1),
                accessor.getDouble(2),
                accessor.getInteger(3),
                OrderStatus.valueOf(accessor.getString(4))
        );
    }

}
