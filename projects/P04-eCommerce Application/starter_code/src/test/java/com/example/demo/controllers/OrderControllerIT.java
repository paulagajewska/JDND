package com.example.demo.controllers;

import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.UserOrder;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.persistence.repositories.OrderRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import static com.example.demo.controllers.TestHelper.getValidJwtToken;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJsonTesters
public class OrderControllerIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    public void submit_http_ok() throws Exception {
        //given
        Cart cart = cartRepository.save(new Cart());
        Optional<Item> item = itemRepository.findById(1l);
        cart.setItems(Lists.list(item.get()));
        User user = userRepository.findByUsername("test_username");
        user.setCart(cart);
        userRepository.save(user);

        //when
        ResultActions resultActions = mvc.perform(
                post(new URI("/api/order/submit/" + user.getUsername()))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .header("Authorization", getValidJwtToken()))
                .andExpect(status().isOk());

        //then
        resultActions
                .andExpect(jsonPath("$.user.id", is((int) user.getId())))
                .andExpect(jsonPath("$.user.username", is(user.getUsername())))
                .andExpect(jsonPath("$.items.[0].id", is(1)))
                .andExpect(jsonPath("$.items.[0].name", is("Round Widget")));
    }

    @Test
    public void submit_http_not_found() throws Exception {
        //expect
        mvc.perform(
                post(new URI("/api/order/submit/submit_invalid_username"))
                        .header("Authorization", getValidJwtToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void submit_http_forbidden() throws Exception {
        //expect
        mvc.perform(
                post(new URI("/api/order/submit/username")))
                .andExpect(status().isForbidden());
    }

    @Test
    public void getOrdersForUser_http_ok() throws Exception {
        //given
        User user = userRepository.findByUsername("test_username");

        Cart cart = new Cart();
        List<Item> items = itemRepository.findByName("Round Widget");
        cart.setItems(items);
        cart.setTotal(BigDecimal.TEN);
        cart.setUser(user);
        cartRepository.save(cart);

        UserOrder order = UserOrder.createFromCart(cart);
        UserOrder saveOrder = orderRepository.save(order);

        //when
        ResultActions resultActions = mvc.perform(
                get(new URI("/api/order/history/" + user.getUsername()))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .header("Authorization", getValidJwtToken()))
                .andExpect(status().isOk());

        //then
        resultActions
                .andExpect(jsonPath("$.[0].user.id", is((int) saveOrder.getUser().getId())))
                .andExpect(jsonPath("$.[0].user.username", is(saveOrder.getUser().getUsername())))
                .andExpect(jsonPath("$.[0].items.[0].id", is(saveOrder.getItems().get(0).getId().intValue())))
                .andExpect(jsonPath("$.[0].items.[0].price", is(saveOrder.getItems().get(0).getPrice().doubleValue())))
                .andExpect(jsonPath("$.[0].items.[0].description", is(saveOrder.getItems().get(0).getDescription())))
                .andExpect(jsonPath("$.[0].items.[0].name", is(saveOrder.getItems().get(0).getName())));
    }

    @Test
    public void getOrdersForUser_http_not_found() throws Exception {
        //expect
        mvc.perform(
                get(new URI("/api/order/history/username_notfound"))
                        .header("Authorization", getValidJwtToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getOrdersForUser_http_forbidden() throws Exception {
        //expect
        mvc.perform(
                get(new URI("/api/order/history/username")))
                .andExpect(status().isForbidden());
    }

}
