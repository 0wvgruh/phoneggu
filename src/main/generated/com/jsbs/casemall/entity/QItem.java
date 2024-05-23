package com.jsbs.casemall.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QItem is a Querydsl query type for Item
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QItem extends EntityPathBase<Product> {

    private static final long serialVersionUID = 2007688347L;

    public static final QItem item = new QItem("item");

    public final QBaseEntity _super = new QBaseEntity(this);

    public final NumberPath<Integer> categoriesNo = createNumber("categoriesNo", Integer.class);

    //inherited
    public final StringPath createdBy = _super.createdBy;

    public final NumberPath<Double> discount = createNumber("discount", Double.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final StringPath modifiedBy = _super.modifiedBy;

    public final StringPath prDetail = createString("prDetail");

    public final StringPath prName = createString("prName");

    public final NumberPath<Integer> prPrice = createNumber("prPrice", Integer.class);

    public final EnumPath<com.jsbs.casemall.constant.ProductSell> prSell = createEnum("prSell", com.jsbs.casemall.constant.ProductSell.class);

    public final NumberPath<Integer> prStock = createNumber("prStock", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> regTime = _super.regTime;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updateTime = _super.updateTime;

    public QItem(String variable) {
        super(Product.class, forVariable(variable));
    }

    public QItem(Path<? extends Product> path) {
        super(path.getType(), path.getMetadata());
    }

    public QItem(PathMetadata metadata) {
        super(Product.class, metadata);
    }

}
