package com.sky.controller.user;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;
import com.sky.result.Result;
import com.sky.service.ShoppingCartService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user/shoppingCart")
@Slf4j
public class ShoppingCartController {
    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 添加购物车
     * @param shoppingCartDTO
     * @return
     */
    @PostMapping("/add")
    public Result add(@RequestBody ShoppingCartDTO shoppingCartDTO){
        log.info("添加购物车,商品信息为:{}",shoppingCartDTO);
        shoppingCartService.addShoppingCart(shoppingCartDTO);
        return Result.success();
    }


    /**
     * 查看购物车
     * @return
     */
    @GetMapping("/list")
    public Result<List<ShoppingCart>> list(){
       log.info("查看购物车");
       List<ShoppingCart> list=shoppingCartService.showShoppingCart();
       return Result.success(list);
    }

    /**
     * 清空购物车
     * @return
     */
    @DeleteMapping("/clean")
    public Result clean(){
        log.info("清空购物车数据");
        shoppingCartService.cleanShoppingCart();
        return Result.success();
    }

    /**
     * 减少购物车中商品数量
     * @param shoppingCartDTO
     * @return
     */
    @PostMapping("/sub")
    public Result subShoppingCartNumber(@RequestBody ShoppingCartDTO shoppingCartDTO){
        log.info("减少购物车商品数量,减少的数据为:{}",shoppingCartDTO);
        shoppingCartService.subShoppingCartNumber(shoppingCartDTO);
        return Result.success();
    }
}
