
MERGE (beijing:KgNode {name: "北京"})
SET beijing.category = "城市",
    beijing.description = "中国首都，文化遗产丰富。",
    beijing.tags = ["历史", "博物馆", "城市漫游"],
    beijing.updatedAt = datetime().toString(),
    beijing.createdAt = coalesce(beijing.createdAt, datetime().toString());

MERGE (forbidden:KgNode {name: "故宫博物院"})
SET forbidden.category = "景点",
    forbidden.description = "明清皇宫，建议至少预留半天。",
    forbidden.tags = ["世界文化遗产", "历史建筑"],
    forbidden.updatedAt = datetime().toString(),
    forbidden.createdAt = coalesce(forbidden.createdAt, datetime().toString());

MERGE (greatWall:KgNode {name: "八达岭长城"})
SET greatWall.category = "景点",
    greatWall.description = "北京热门长城段，适合首次打卡。",
    greatWall.tags = ["户外", "历史遗迹"],
    greatWall.updatedAt = datetime().toString(),
    greatWall.createdAt = coalesce(greatWall.createdAt, datetime().toString());

MERGE (hotpot:KgNode {name: "北京铜锅涮肉"})
SET hotpot.category = "美食",
    hotpot.description = "代表性北方火锅，冬季体验更佳。",
    hotpot.tags = ["美食", "老字号"],
    hotpot.updatedAt = datetime().toString(),
    hotpot.createdAt = coalesce(hotpot.createdAt, datetime().toString());

MERGE (shanghai:KgNode {name: "上海"})
SET shanghai.category = "城市",
    shanghai.description = "兼具现代都市风貌与近代历史街区。",
    shanghai.tags = ["城市漫游", "博物馆", "建筑"],
    shanghai.updatedAt = datetime().toString(),
    shanghai.createdAt = coalesce(shanghai.createdAt, datetime().toString());

MERGE (bund:KgNode {name: "外滩"})
SET bund.category = "景点",
    bund.description = "黄浦江沿岸地标，夜景观赏体验佳。",
    bund.tags = ["夜景", "建筑"],
    bund.updatedAt = datetime().toString(),
    bund.createdAt = coalesce(bund.createdAt, datetime().toString());

MERGE (disney:KgNode {name: "上海迪士尼度假区"})
SET disney.category = "景点",
    disney.description = "亲子和情侣热门主题乐园。",
    disney.tags = ["亲子", "乐园"],
    disney.updatedAt = datetime().toString(),
    disney.createdAt = coalesce(disney.createdAt, datetime().toString());

MERGE (chengdu:KgNode {name: "成都"})
SET chengdu.category = "城市",
    chengdu.description = "以休闲生活、美食与熊猫文化著称。",
    chengdu.tags = ["美食", "慢生活"],
    chengdu.updatedAt = datetime().toString(),
    chengdu.createdAt = coalesce(chengdu.createdAt, datetime().toString());

MERGE (panda:KgNode {name: "成都大熊猫繁育研究基地"})
SET panda.category = "景点",
    panda.description = "近距离了解熊猫生态与保育。",
    panda.tags = ["亲子", "动物"],
    panda.updatedAt = datetime().toString(),
    panda.createdAt = coalesce(panda.createdAt, datetime().toString());

MERGE (hotpotChengdu:KgNode {name: "成都火锅"})
SET hotpotChengdu.category = "美食",
    hotpotChengdu.description = "麻辣口味鲜明，建议按辣度点单。",
    hotpotChengdu.tags = ["美食", "川菜"],
    hotpotChengdu.updatedAt = datetime().toString(),
    hotpotChengdu.createdAt = coalesce(hotpotChengdu.createdAt, datetime().toString());

MERGE (beijing)-[r1:KG_REL]->(forbidden)
SET r1.predicate = "包含景点",
    r1.description = "故宫位于北京市中心区域",
    r1.weight = 0.95,
    r1.updatedAt = datetime().toString(),
    r1.createdAt = coalesce(r1.createdAt, datetime().toString());

MERGE (beijing)-[r2:KG_REL]->(greatWall)
SET r2.predicate = "包含景点",
    r2.description = "八达岭为北京最常见长城游览段",
    r2.weight = 0.93,
    r2.updatedAt = datetime().toString(),
    r2.createdAt = coalesce(r2.createdAt, datetime().toString());

MERGE (beijing)-[r3:KG_REL]->(hotpot)
SET r3.predicate = "推荐美食",
    r3.description = "北京传统涮肉体验",
    r3.weight = 0.86,
    r3.updatedAt = datetime().toString(),
    r3.createdAt = coalesce(r3.createdAt, datetime().toString());

MERGE (shanghai)-[r4:KG_REL]->(bund)
SET r4.predicate = "包含景点",
    r4.description = "外滩是上海经典城市地标",
    r4.weight = 0.94,
    r4.updatedAt = datetime().toString(),
    r4.createdAt = coalesce(r4.createdAt, datetime().toString());

MERGE (shanghai)-[r5:KG_REL]->(disney)
SET r5.predicate = "亲子推荐",
    r5.description = "上海迪士尼适合全天行程",
    r5.weight = 0.9,
    r5.updatedAt = datetime().toString(),
    r5.createdAt = coalesce(r5.createdAt, datetime().toString());

MERGE (chengdu)-[r6:KG_REL]->(panda)
SET r6.predicate = "包含景点",
    r6.description = "成都熊猫基地建议上午前往",
    r6.weight = 0.92,
    r6.updatedAt = datetime().toString(),
    r6.createdAt = coalesce(r6.createdAt, datetime().toString());

MERGE (chengdu)-[r7:KG_REL]->(hotpotChengdu)
SET r7.predicate = "推荐美食",
    r7.description = "成都火锅可按微辣起步",
    r7.weight = 0.88,
    r7.updatedAt = datetime().toString(),
    r7.createdAt = coalesce(r7.createdAt, datetime().toString());

MERGE (forbidden)-[r8:KG_REL]->(greatWall)
SET r8.predicate = "可同城串联",
    r8.description = "北京 2-3 日行程常见搭配",
    r8.weight = 0.75,
    r8.updatedAt = datetime().toString(),
    r8.createdAt = coalesce(r8.createdAt, datetime().toString());
