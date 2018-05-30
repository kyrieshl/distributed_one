package com.litemall.distributed_one.web;

import org.apache.commons.lang3.ObjectUtils;
import org.linlinjava.litemall.db.domain.LitemallComment;
import org.linlinjava.litemall.db.domain.LitemallOrder;
import org.linlinjava.litemall.db.domain.LitemallOrderGoods;
import org.linlinjava.litemall.db.service.*;
import org.linlinjava.litemall.db.util.JacksonUtil;
import org.linlinjava.litemall.db.util.OrderUtil;
import org.linlinjava.litemall.db.util.ResponseUtil;
import com.litemall.distributed_one.annotation.LoginUser;
import com.litemall.distributed_one.dao.UserInfo;
import com.litemall.distributed_one.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wx/comment")
public class WxCommentController {
    @Autowired
    private LitemallCommentService commentService;
    @Autowired
    private LitemallOrderService orderService;
    @Autowired
    private LitemallOrderGoodsService orderGoodsService;
    @Autowired
    private LitemallUserService userService;
    @Autowired
    private LitemallCouponService couponService;
    @Autowired
    private UserInfoService userInfoService;

    /**
     * 发表评论
     *
     * TODO, 对于评论，应该检测用户是否有权限评论。
     * 1. 如果用户没有购买过商品，则不能发表对该商品的评论
     * 2. 如果用户购买商品后规定时间内没有评论，则过期也不能再评论
     *
     * @param userId 用户Id
     * @param body 评论内容、订单Id
     * @return 发表评论操作结果
     * 成功则
     *  {
     *      errno: 0,
     *      errmsg: '成功',
     *      data: xxx
     *  }
     *   失败则 { errno: XXX, errmsg: XXX }
     */
    @PostMapping("post")
    public Object post(@LoginUser Integer userId,@RequestBody String body) {
        if(userId == null){
            return ResponseUtil.unlogin();
        }
        LitemallComment comment = new LitemallComment();
        comment.setContent(JacksonUtil.parseString(body,"content"));
        comment.setHasPicture(JacksonUtil.parseBoolean(body,"hasPicture"));
        comment.setPicUrls(JacksonUtil.parseStringList(body,"picUrls"));
        comment.setStar(JacksonUtil.parseShort(body,"star"));
        comment.setTypeId(JacksonUtil.parseByte(body,"typeId"));
        comment.setValueId(JacksonUtil.parseInteger(body,"valueId"));
        comment.setAddTime(LocalDateTime.now());
        comment.setUserId(userId);
        if(comment == null){
            return ResponseUtil.badArgument();
        }
        commentService.save(comment);
d
        Integer orderId = JacksonUtil.parseInteger(body,"orderId");
//       Integer orderId = Integer.parseInt(request.getParameter("orderId"));
        LitemallOrder order = orderService.findById(orderId);
        if(order == null){
            return ResponseUtil.badArgument();
        }
        if(!order.getUserId().equals(userId)){
            return ResponseUtil.badArgumentValue();
        }
        order.setOrderStatus(OrderUtil.STATUS_EVALUATE);
        orderService.update(order);

//        设置订单具体商品评价状态
        Integer goodsId = JacksonUtil.parseInteger(body,"valueId");
        LitemallOrderGoods orderGoods = orderGoodsService.findByOidAndGid(orderId,goodsId).get(0);
        orderGoods.setEvaluateFlag(false);
        orderGoodsService.update(orderGoods);

        return ResponseUtil.ok(comment);
    }

    /**
     * 评论数量
     *
     * @param typeId 类型ID。 如果是0，则查询商品评论；如果是1，则查询专题评论。
     * @param valueId 商品或专题ID。如果typeId是0，则是商品ID；如果typeId是1，则是专题ID。
     * @return 评论数量
     *   成功则
     *  {
     *      errno: 0,
     *      errmsg: '成功',
     *      data:
     *          {
     *              allCount: xxx,
     *              hasPicCount: xxx
     *          }
     *  }
     *   失败则 { errno: XXX, errmsg: XXX }
     */
    @GetMapping("count")
    public Object count(Byte typeId, Integer valueId) {
        int allCount = commentService.count(typeId, valueId, 0, 0, 0);
        int hasPicCount = commentService.count(typeId, valueId, 1, 0, 0);
        Map<String, Object> data = new HashMap();
        data.put("allCount", allCount);
        data.put("hasPicCount", hasPicCount);
        return ResponseUtil.ok(data);
    }

    /**
     * 评论列表
     *
     * @param typeId 类型ID。 如果是0，则查询商品评论；如果是1，则查询专题评论。
     * @param valueId 商品或专题ID。如果typeId是0，则是商品ID；如果typeId是1，则是专题ID。
     * @param showType 显示类型。如果是0，则查询全部；如果是1，则查询有图片的评论。
     * @param page 分页页数
     * @param size 分页大小
     * @return 评论列表
     *   成功则
     *  {
     *      errno: 0,
     *      errmsg: '成功',
     *      data:
     *          {
     *              data: xxx,
     *              count: xxx，
     *              currentPage: xxx
     *          }
     *  }
     *   失败则 { errno: XXX, errmsg: XXX }
     */
    @GetMapping("list")
    public Object list(Byte typeId, Integer valueId, Integer showType,
                       @RequestParam(value = "page", defaultValue = "1") Integer page,
                       @RequestParam(value = "size", defaultValue = "10") Integer size) {
        if(!ObjectUtils.allNotNull(typeId, valueId, showType)){
            return ResponseUtil.badArgument();
        }

        List<LitemallComment> commentList = commentService.query(typeId, valueId, showType, page, size);
        int count = commentService.count(typeId, valueId, showType, page, size);

        List<Map<String, Object>> commentVoList = new ArrayList<>(commentList.size());
        for(LitemallComment comment : commentList){
            Map<String, Object> commentVo = new HashMap<>();
            UserInfo userInfo = userInfoService.getInfo(comment.getUserId());
            commentVo.put("userInfo", userInfo);
            commentVo.put("addTime", comment.getAddTime());
            commentVo.put("content",comment.getContent());
            commentVo.put("picList", comment.getPicUrls());

            commentVoList.add(commentVo);
        }
        Map<String, Object> data = new HashMap();
        data.put("data", commentVoList);
        data.put("count", count);
        data.put("currentPage", page);
        return ResponseUtil.ok(data);
    }
}