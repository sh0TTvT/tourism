
MERGE (n:KgNode {wikidataId: 'Q148'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: '中华人民共和国', category: '国家', description: '东亚国家', aliases: ['中国', '中', '红色中国', '共产中国', '中国大陆', '大陆', '陆', '新中国'], tags: ['真实来源', 'Wikidata', '国家'], source: 'Wikidata', sourceUrl: 'https://www.wikidata.org/wiki/Q148', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'Q148', entityType: 'country'};

MERGE (n:KgNode {wikidataId: 'Q1660063'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: '成都地铁', category: '城市', description: '中国成都市的城市轨道交通系统', aliases: ['成都地鐵'], tags: ['真实来源', 'Wikidata', '城市'], source: 'Wikidata', sourceUrl: 'https://www.wikidata.org/wiki/Q1660063', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'Q1660063', entityType: 'city', latitude: 30.66, longitude: 104.063333, wikipediaUrl: 'https://zh.wikipedia.org/wiki/%E6%88%90%E9%83%BD%E5%9C%B0%E9%93%81'};

MERGE (n:KgNode {wikidataId: 'Q8686'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: '上海市', category: '城市', description: '中华人民共和国直辖市', aliases: ['上海', '沪', '申', '滬', '中國上海', '中国上海', '申城', 'Siō̤ng-hâi-chī'], tags: ['真实来源', 'Wikidata', '城市'], source: 'Wikidata', sourceUrl: 'https://www.wikidata.org/wiki/Q8686', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'Q8686', entityType: 'city', latitude: 31.2325, longitude: 121.469167, population: 23390000, wikipediaUrl: 'https://zh.wikipedia.org/wiki/%E4%B8%8A%E6%B5%B7%E5%B8%82'};

MERGE (n:KgNode {wikidataId: 'Q956'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: '北京市', category: '城市', description: '中華人民共和國首都暨直轄市', aliases: ['北京', '京', '北平', '北平市', '平', '燕京', '燕', '京兆地方'], tags: ['真实来源', 'Wikidata', '城市'], source: 'Wikidata', sourceUrl: 'https://www.wikidata.org/wiki/Q956', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'Q956', entityType: 'city', latitude: 39.90403, longitude: 116.407526, population: 19612368, wikipediaUrl: 'https://zh.wikipedia.org/wiki/%E5%8C%97%E4%BA%AC%E5%B8%82'};

MERGE (n:KgNode {wikidataId: 'WEB_00b69865aae151cc'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: '上海', category: '来源页面', description: '旅行指南页面（来自 Wikidata sitelink）', aliases: [], tags: ['真实来源', '网页来源', '旅行指南', 'zh.wikivoyage.org'], source: 'WikidataSitelink', sourceUrl: 'https://zh.wikivoyage.org/wiki/%E4%B8%8A%E6%B5%B7', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'WEB_00b69865aae151cc', entityType: 'web_source', sourceType: '旅行指南', sourceDomain: 'zh.wikivoyage.org', sourceProvider: 'WikidataSitelink', cityWikidataId: 'Q8686'};

MERGE (n:KgNode {wikidataId: 'WEB_10cf60906b9b3ca2'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: 'Beijing', category: '来源页面', description: '百科页面（来自 Wikidata sitelink）', aliases: [], tags: ['真实来源', '网页来源', '百科', 'en.wikipedia.org'], source: 'WikidataSitelink', sourceUrl: 'https://en.wikipedia.org/wiki/Beijing', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'WEB_10cf60906b9b3ca2', entityType: 'web_source', sourceType: '百科', sourceDomain: 'en.wikipedia.org', sourceProvider: 'WikidataSitelink', cityWikidataId: 'Q956'};

MERGE (n:KgNode {wikidataId: 'WEB_1b6a41adcaaa4e7e'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: '北京市 官方网站', category: '来源页面', description: 'Wikidata 标注的官方站点', aliases: [], tags: ['真实来源', '网页来源', '官网', 'english.beijing.gov.cn'], source: 'WikidataClaim', sourceUrl: 'http://english.beijing.gov.cn/', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'WEB_1b6a41adcaaa4e7e', entityType: 'web_source', sourceType: '官网', sourceDomain: 'english.beijing.gov.cn', sourceProvider: 'WikidataClaim', cityWikidataId: 'Q956'};

MERGE (n:KgNode {wikidataId: 'WEB_2dd6cbecbe8db9bd'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: '成都地铁 官方网站', category: '来源页面', description: 'Wikidata 标注的官方站点', aliases: [], tags: ['真实来源', '网页来源', '官网', 'www.chengdurail.com'], source: 'WikidataClaim', sourceUrl: 'https://www.chengdurail.com/', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'WEB_2dd6cbecbe8db9bd', entityType: 'web_source', sourceType: '官网', sourceDomain: 'www.chengdurail.com', sourceProvider: 'WikidataClaim', cityWikidataId: 'Q1660063'};

MERGE (n:KgNode {wikidataId: 'WEB_31fba8d7846be748'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: '北京市 - 维基百科，自由的百科全书', category: '来源页面', description: '北京市 百科数据来源页面', aliases: [], tags: ['真实来源', '网页来源', '百科', 'zh.wikipedia.org'], source: 'DuckDuckGo', sourceUrl: 'https://zh.wikipedia.org/wiki/北京市', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'WEB_31fba8d7846be748', entityType: 'web_source', sourceType: '百科', sourceDomain: 'zh.wikipedia.org', sourceProvider: 'DuckDuckGo', cityWikidataId: 'Q956'};

MERGE (n:KgNode {wikidataId: 'WEB_322cdbdce3402119'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: '北京', category: '来源页面', description: '旅行指南页面（来自 Wikidata sitelink）', aliases: [], tags: ['真实来源', '网页来源', '旅行指南', 'zh.wikivoyage.org'], source: 'WikidataSitelink', sourceUrl: 'https://zh.wikivoyage.org/wiki/%E5%8C%97%E4%BA%AC', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'WEB_322cdbdce3402119', entityType: 'web_source', sourceType: '旅行指南', sourceDomain: 'zh.wikivoyage.org', sourceProvider: 'WikidataSitelink', cityWikidataId: 'Q956'};

MERGE (n:KgNode {wikidataId: 'WEB_3ba6024e321400e1'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: '北京市_百度百科', category: '来源页面', description: '北京市 百科数据来源页面', aliases: [], tags: ['真实来源', '网页来源', '百科', 'baike.baidu.com'], source: 'DuckDuckGo', sourceUrl: 'https://baike.baidu.com/item/北京市/65090896', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'WEB_3ba6024e321400e1', entityType: 'web_source', sourceType: '百科', sourceDomain: 'baike.baidu.com', sourceProvider: 'DuckDuckGo', cityWikidataId: 'Q956'};

MERGE (n:KgNode {wikidataId: 'WEB_481368f3c8819752'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: 'Category:北京市旅游景点 - 维基百科，自由的百科全书', category: '来源页面', description: '北京市 百科数据来源页面', aliases: [], tags: ['真实来源', '网页来源', '百科', 'zh.wikipedia.org'], source: 'DuckDuckGo', sourceUrl: 'https://zh.wikipedia.org/wiki/Category:北京市旅游景点', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'WEB_481368f3c8819752', entityType: 'web_source', sourceType: '百科', sourceDomain: 'zh.wikipedia.org', sourceProvider: 'DuckDuckGo', cityWikidataId: 'Q956'};

MERGE (n:KgNode {wikidataId: 'WEB_48ca4114388d3148'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: '上海市 官方网站', category: '来源页面', description: 'Wikidata 标注的官方站点', aliases: [], tags: ['真实来源', '网页来源', '官网', 'english.shanghai.gov.cn'], source: 'WikidataClaim', sourceUrl: 'https://english.shanghai.gov.cn/nw46669/index.html', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'WEB_48ca4114388d3148', entityType: 'web_source', sourceType: '官网', sourceDomain: 'english.shanghai.gov.cn', sourceProvider: 'WikidataClaim', cityWikidataId: 'Q8686'};

MERGE (n:KgNode {wikidataId: 'WEB_4aa522519955ef8d'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: 'Shanghai', category: '来源页面', description: '百科页面（来自 Wikidata sitelink）', aliases: [], tags: ['真实来源', '网页来源', '百科', 'en.wikipedia.org'], source: 'WikidataSitelink', sourceUrl: 'https://en.wikipedia.org/wiki/Shanghai', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'WEB_4aa522519955ef8d', entityType: 'web_source', sourceType: '百科', sourceDomain: 'en.wikipedia.org', sourceProvider: 'WikidataSitelink', cityWikidataId: 'Q8686'};

MERGE (n:KgNode {wikidataId: 'WEB_5e08745d31f03a2a'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: '上海市', category: '来源页面', description: '百科页面（来自 Wikidata sitelink）', aliases: [], tags: ['真实来源', '网页来源', '百科', 'zh.wikipedia.org'], source: 'WikidataSitelink', sourceUrl: 'https://zh.wikipedia.org/wiki/%E4%B8%8A%E6%B5%B7%E5%B8%82', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'WEB_5e08745d31f03a2a', entityType: 'web_source', sourceType: '百科', sourceDomain: 'zh.wikipedia.org', sourceProvider: 'WikidataSitelink', cityWikidataId: 'Q8686'};

MERGE (n:KgNode {wikidataId: 'WEB_616e44a32f638ade'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: '成都地铁', category: '来源页面', description: '百科页面（来自 Wikidata sitelink）', aliases: [], tags: ['真实来源', '网页来源', '百科', 'zh.wikipedia.org'], source: 'WikidataSitelink', sourceUrl: 'https://zh.wikipedia.org/wiki/%E6%88%90%E9%83%BD%E5%9C%B0%E9%93%81', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'WEB_616e44a32f638ade', entityType: 'web_source', sourceType: '百科', sourceDomain: 'zh.wikipedia.org', sourceProvider: 'WikidataSitelink', cityWikidataId: 'Q1660063'};

MERGE (n:KgNode {wikidataId: 'WEB_70ee750bdd8a2634'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: 'Beijing', category: '来源页面', description: '旅行指南页面（来自 Wikidata sitelink）', aliases: [], tags: ['真实来源', '网页来源', '旅行指南', 'en.wikivoyage.org'], source: 'WikidataSitelink', sourceUrl: 'https://en.wikivoyage.org/wiki/Beijing', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'WEB_70ee750bdd8a2634', entityType: 'web_source', sourceType: '旅行指南', sourceDomain: 'en.wikivoyage.org', sourceProvider: 'WikidataSitelink', cityWikidataId: 'Q956'};

MERGE (n:KgNode {wikidataId: 'WEB_7ed67cdcbba4f8f1'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: 'Shanghai', category: '来源页面', description: '旅行指南页面（来自 Wikidata sitelink）', aliases: [], tags: ['真实来源', '网页来源', '旅行指南', 'en.wikivoyage.org'], source: 'WikidataSitelink', sourceUrl: 'https://en.wikivoyage.org/wiki/Shanghai', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'WEB_7ed67cdcbba4f8f1', entityType: 'web_source', sourceType: '旅行指南', sourceDomain: 'en.wikivoyage.org', sourceProvider: 'WikidataSitelink', cityWikidataId: 'Q8686'};

MERGE (n:KgNode {wikidataId: 'WEB_89849c1a61f9103f'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: '北京市_百度百科', category: '来源页面', description: '北京市 百科数据来源页面', aliases: [], tags: ['真实来源', '网页来源', '百科', 'baike.baidu.com'], source: 'DuckDuckGo', sourceUrl: 'https://baike.baidu.com/item/北京市/126069', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'WEB_89849c1a61f9103f', entityType: 'web_source', sourceType: '百科', sourceDomain: 'baike.baidu.com', sourceProvider: 'DuckDuckGo', cityWikidataId: 'Q956'};

MERGE (n:KgNode {wikidataId: 'WEB_89adbf6e9ef08182'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: '北京市 参考页面', category: '来源页面', description: 'Wikidata 引用的补充来源', aliases: [], tags: ['真实来源', '网页来源', '参考', 'baike.sogou.com'], source: 'WikidataClaim', sourceUrl: 'https://baike.sogou.com/v4412.htm', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'WEB_89adbf6e9ef08182', entityType: 'web_source', sourceType: '参考', sourceDomain: 'baike.sogou.com', sourceProvider: 'WikidataClaim', cityWikidataId: 'Q956'};

MERGE (n:KgNode {wikidataId: 'WEB_a55248a498c982bc'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: '北京市', category: '来源页面', description: '百科页面（来自 Wikidata sitelink）', aliases: [], tags: ['真实来源', '网页来源', '百科', 'zh.wikipedia.org'], source: 'WikidataSitelink', sourceUrl: 'https://zh.wikipedia.org/wiki/%E5%8C%97%E4%BA%AC%E5%B8%82', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'WEB_a55248a498c982bc', entityType: 'web_source', sourceType: '百科', sourceDomain: 'zh.wikipedia.org', sourceProvider: 'WikidataSitelink', cityWikidataId: 'Q956'};

MERGE (n:KgNode {wikidataId: 'WEB_a8adee8b4531332f'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: '北京市 官方网站', category: '来源页面', description: 'Wikidata 标注的官方站点', aliases: [], tags: ['真实来源', '网页来源', '官网', 'www.beijing.gov.cn'], source: 'WikidataClaim', sourceUrl: 'http://www.beijing.gov.cn/gate/big5/www.beijing.gov.cn/index.html', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'WEB_a8adee8b4531332f', entityType: 'web_source', sourceType: '官网', sourceDomain: 'www.beijing.gov.cn', sourceProvider: 'WikidataClaim', cityWikidataId: 'Q956'};

MERGE (n:KgNode {wikidataId: 'WEB_ae0e97263d8b4306'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: '上海市 官方网站', category: '来源页面', description: 'Wikidata 标注的官方站点', aliases: [], tags: ['真实来源', '网页来源', '官网', 'www.shanghai.gov.cn'], source: 'WikidataClaim', sourceUrl: 'https://www.shanghai.gov.cn/', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'WEB_ae0e97263d8b4306', entityType: 'web_source', sourceType: '官网', sourceDomain: 'www.shanghai.gov.cn', sourceProvider: 'WikidataClaim', cityWikidataId: 'Q8686'};

MERGE (n:KgNode {wikidataId: 'WEB_d54bcd0264adc8fe'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: '北京市 官方网站', category: '来源页面', description: 'Wikidata 标注的官方站点', aliases: [], tags: ['真实来源', '网页来源', '官网', 'www.beijing.gov.cn'], source: 'WikidataClaim', sourceUrl: 'https://www.beijing.gov.cn/', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'WEB_d54bcd0264adc8fe', entityType: 'web_source', sourceType: '官网', sourceDomain: 'www.beijing.gov.cn', sourceProvider: 'WikidataClaim', cityWikidataId: 'Q956'};

MERGE (n:KgNode {wikidataId: 'WEB_db8a633b794145c0'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: '北京市 官方网站', category: '来源页面', description: 'Wikidata 标注的官方站点', aliases: [], tags: ['真实来源', '网页来源', '官网', 'french.beijing.gov.cn'], source: 'WikidataClaim', sourceUrl: 'http://french.beijing.gov.cn/', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'WEB_db8a633b794145c0', entityType: 'web_source', sourceType: '官网', sourceDomain: 'french.beijing.gov.cn', sourceProvider: 'WikidataClaim', cityWikidataId: 'Q956'};

MERGE (n:KgNode {wikidataId: 'WEB_f0c8f303bcd8d1f3'})
ON CREATE SET n.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET n += {name: 'Chengdu Metro', category: '来源页面', description: '百科页面（来自 Wikidata sitelink）', aliases: [], tags: ['真实来源', '网页来源', '百科', 'en.wikipedia.org'], source: 'WikidataSitelink', sourceUrl: 'https://en.wikipedia.org/wiki/Chengdu_Metro', updatedAt: '2026-03-03T09:19:47.458345+00:00', wikidataId: 'WEB_f0c8f303bcd8d1f3', entityType: 'web_source', sourceType: '百科', sourceDomain: 'en.wikipedia.org', sourceProvider: 'WikidataSitelink', cityWikidataId: 'Q1660063'};

MATCH (a:KgNode {wikidataId: 'Q1660063'})
MATCH (b:KgNode {wikidataId: 'WEB_2dd6cbecbe8db9bd'})
MERGE (a)-[r:KG_REL {predicate: '官方信息', targetWikidataId: 'WEB_2dd6cbecbe8db9bd'}]->(b)
ON CREATE SET r.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET r += {predicate: '官方信息', description: '成都地铁 的 官网来源：成都地铁 官方网站', weight: 0.9, source: 'WikidataClaim', sourceUrl: 'https://www.chengdurail.com/', updatedAt: '2026-03-03T09:19:47.458345+00:00'};

MATCH (a:KgNode {wikidataId: 'Q1660063'})
MATCH (b:KgNode {wikidataId: 'Q148'})
MERGE (a)-[r:KG_REL {predicate: '所属国家', targetWikidataId: 'Q148'}]->(b)
ON CREATE SET r.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET r += {predicate: '所属国家', description: '成都地铁 位于 中华人民共和国', weight: 0.99, source: 'Wikidata', sourceUrl: 'https://www.wikidata.org/wiki/Q1660063', updatedAt: '2026-03-03T09:19:47.458345+00:00'};

MATCH (a:KgNode {wikidataId: 'Q1660063'})
MATCH (b:KgNode {wikidataId: 'WEB_616e44a32f638ade'})
MERGE (a)-[r:KG_REL {predicate: '百科参考', targetWikidataId: 'WEB_616e44a32f638ade'}]->(b)
ON CREATE SET r.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET r += {predicate: '百科参考', description: '成都地铁 的 百科来源：成都地铁', weight: 0.74, source: 'WikidataSitelink', sourceUrl: 'https://zh.wikipedia.org/wiki/%E6%88%90%E9%83%BD%E5%9C%B0%E9%93%81', updatedAt: '2026-03-03T09:19:47.458345+00:00'};

MATCH (a:KgNode {wikidataId: 'Q1660063'})
MATCH (b:KgNode {wikidataId: 'WEB_f0c8f303bcd8d1f3'})
MERGE (a)-[r:KG_REL {predicate: '百科参考', targetWikidataId: 'WEB_f0c8f303bcd8d1f3'}]->(b)
ON CREATE SET r.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET r += {predicate: '百科参考', description: '成都地铁 的 百科来源：Chengdu Metro', weight: 0.74, source: 'WikidataSitelink', sourceUrl: 'https://en.wikipedia.org/wiki/Chengdu_Metro', updatedAt: '2026-03-03T09:19:47.458345+00:00'};

MATCH (a:KgNode {wikidataId: 'Q8686'})
MATCH (b:KgNode {wikidataId: 'WEB_48ca4114388d3148'})
MERGE (a)-[r:KG_REL {predicate: '官方信息', targetWikidataId: 'WEB_48ca4114388d3148'}]->(b)
ON CREATE SET r.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET r += {predicate: '官方信息', description: '上海市 的 官网来源：上海市 官方网站', weight: 0.9, source: 'WikidataClaim', sourceUrl: 'https://english.shanghai.gov.cn/nw46669/index.html', updatedAt: '2026-03-03T09:19:47.458345+00:00'};

MATCH (a:KgNode {wikidataId: 'Q8686'})
MATCH (b:KgNode {wikidataId: 'WEB_ae0e97263d8b4306'})
MERGE (a)-[r:KG_REL {predicate: '官方信息', targetWikidataId: 'WEB_ae0e97263d8b4306'}]->(b)
ON CREATE SET r.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET r += {predicate: '官方信息', description: '上海市 的 官网来源：上海市 官方网站', weight: 0.9, source: 'WikidataClaim', sourceUrl: 'https://www.shanghai.gov.cn/', updatedAt: '2026-03-03T09:19:47.458345+00:00'};

MATCH (a:KgNode {wikidataId: 'Q8686'})
MATCH (b:KgNode {wikidataId: 'Q148'})
MERGE (a)-[r:KG_REL {predicate: '所属国家', targetWikidataId: 'Q148'}]->(b)
ON CREATE SET r.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET r += {predicate: '所属国家', description: '上海市 位于 中华人民共和国', weight: 0.99, source: 'Wikidata', sourceUrl: 'https://www.wikidata.org/wiki/Q8686', updatedAt: '2026-03-03T09:19:47.458345+00:00'};

MATCH (a:KgNode {wikidataId: 'Q8686'})
MATCH (b:KgNode {wikidataId: 'WEB_00b69865aae151cc'})
MERGE (a)-[r:KG_REL {predicate: '旅行指南', targetWikidataId: 'WEB_00b69865aae151cc'}]->(b)
ON CREATE SET r.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET r += {predicate: '旅行指南', description: '上海市 的 旅行指南来源：上海', weight: 0.76, source: 'WikidataSitelink', sourceUrl: 'https://zh.wikivoyage.org/wiki/%E4%B8%8A%E6%B5%B7', updatedAt: '2026-03-03T09:19:47.458345+00:00'};

MATCH (a:KgNode {wikidataId: 'Q8686'})
MATCH (b:KgNode {wikidataId: 'WEB_7ed67cdcbba4f8f1'})
MERGE (a)-[r:KG_REL {predicate: '旅行指南', targetWikidataId: 'WEB_7ed67cdcbba4f8f1'}]->(b)
ON CREATE SET r.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET r += {predicate: '旅行指南', description: '上海市 的 旅行指南来源：Shanghai', weight: 0.76, source: 'WikidataSitelink', sourceUrl: 'https://en.wikivoyage.org/wiki/Shanghai', updatedAt: '2026-03-03T09:19:47.458345+00:00'};

MATCH (a:KgNode {wikidataId: 'Q8686'})
MATCH (b:KgNode {wikidataId: 'WEB_4aa522519955ef8d'})
MERGE (a)-[r:KG_REL {predicate: '百科参考', targetWikidataId: 'WEB_4aa522519955ef8d'}]->(b)
ON CREATE SET r.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET r += {predicate: '百科参考', description: '上海市 的 百科来源：Shanghai', weight: 0.74, source: 'WikidataSitelink', sourceUrl: 'https://en.wikipedia.org/wiki/Shanghai', updatedAt: '2026-03-03T09:19:47.458345+00:00'};

MATCH (a:KgNode {wikidataId: 'Q8686'})
MATCH (b:KgNode {wikidataId: 'WEB_5e08745d31f03a2a'})
MERGE (a)-[r:KG_REL {predicate: '百科参考', targetWikidataId: 'WEB_5e08745d31f03a2a'}]->(b)
ON CREATE SET r.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET r += {predicate: '百科参考', description: '上海市 的 百科来源：上海市', weight: 0.74, source: 'WikidataSitelink', sourceUrl: 'https://zh.wikipedia.org/wiki/%E4%B8%8A%E6%B5%B7%E5%B8%82', updatedAt: '2026-03-03T09:19:47.458345+00:00'};

MATCH (a:KgNode {wikidataId: 'Q956'})
MATCH (b:KgNode {wikidataId: 'WEB_1b6a41adcaaa4e7e'})
MERGE (a)-[r:KG_REL {predicate: '官方信息', targetWikidataId: 'WEB_1b6a41adcaaa4e7e'}]->(b)
ON CREATE SET r.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET r += {predicate: '官方信息', description: '北京市 的 官网来源：北京市 官方网站', weight: 0.9, source: 'WikidataClaim', sourceUrl: 'http://english.beijing.gov.cn/', updatedAt: '2026-03-03T09:19:47.458345+00:00'};

MATCH (a:KgNode {wikidataId: 'Q956'})
MATCH (b:KgNode {wikidataId: 'WEB_a8adee8b4531332f'})
MERGE (a)-[r:KG_REL {predicate: '官方信息', targetWikidataId: 'WEB_a8adee8b4531332f'}]->(b)
ON CREATE SET r.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET r += {predicate: '官方信息', description: '北京市 的 官网来源：北京市 官方网站', weight: 0.9, source: 'WikidataClaim', sourceUrl: 'http://www.beijing.gov.cn/gate/big5/www.beijing.gov.cn/index.html', updatedAt: '2026-03-03T09:19:47.458345+00:00'};

MATCH (a:KgNode {wikidataId: 'Q956'})
MATCH (b:KgNode {wikidataId: 'WEB_d54bcd0264adc8fe'})
MERGE (a)-[r:KG_REL {predicate: '官方信息', targetWikidataId: 'WEB_d54bcd0264adc8fe'}]->(b)
ON CREATE SET r.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET r += {predicate: '官方信息', description: '北京市 的 官网来源：北京市 官方网站', weight: 0.9, source: 'WikidataClaim', sourceUrl: 'https://www.beijing.gov.cn/', updatedAt: '2026-03-03T09:19:47.458345+00:00'};

MATCH (a:KgNode {wikidataId: 'Q956'})
MATCH (b:KgNode {wikidataId: 'WEB_db8a633b794145c0'})
MERGE (a)-[r:KG_REL {predicate: '官方信息', targetWikidataId: 'WEB_db8a633b794145c0'}]->(b)
ON CREATE SET r.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET r += {predicate: '官方信息', description: '北京市 的 官网来源：北京市 官方网站', weight: 0.9, source: 'WikidataClaim', sourceUrl: 'http://french.beijing.gov.cn/', updatedAt: '2026-03-03T09:19:47.458345+00:00'};

MATCH (a:KgNode {wikidataId: 'Q956'})
MATCH (b:KgNode {wikidataId: 'Q148'})
MERGE (a)-[r:KG_REL {predicate: '所属国家', targetWikidataId: 'Q148'}]->(b)
ON CREATE SET r.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET r += {predicate: '所属国家', description: '北京市 位于 中华人民共和国', weight: 0.99, source: 'Wikidata', sourceUrl: 'https://www.wikidata.org/wiki/Q956', updatedAt: '2026-03-03T09:19:47.458345+00:00'};

MATCH (a:KgNode {wikidataId: 'Q956'})
MATCH (b:KgNode {wikidataId: 'WEB_322cdbdce3402119'})
MERGE (a)-[r:KG_REL {predicate: '旅行指南', targetWikidataId: 'WEB_322cdbdce3402119'}]->(b)
ON CREATE SET r.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET r += {predicate: '旅行指南', description: '北京市 的 旅行指南来源：北京', weight: 0.76, source: 'WikidataSitelink', sourceUrl: 'https://zh.wikivoyage.org/wiki/%E5%8C%97%E4%BA%AC', updatedAt: '2026-03-03T09:19:47.458345+00:00'};

MATCH (a:KgNode {wikidataId: 'Q956'})
MATCH (b:KgNode {wikidataId: 'WEB_70ee750bdd8a2634'})
MERGE (a)-[r:KG_REL {predicate: '旅行指南', targetWikidataId: 'WEB_70ee750bdd8a2634'}]->(b)
ON CREATE SET r.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET r += {predicate: '旅行指南', description: '北京市 的 旅行指南来源：Beijing', weight: 0.76, source: 'WikidataSitelink', sourceUrl: 'https://en.wikivoyage.org/wiki/Beijing', updatedAt: '2026-03-03T09:19:47.458345+00:00'};

MATCH (a:KgNode {wikidataId: 'Q956'})
MATCH (b:KgNode {wikidataId: 'WEB_10cf60906b9b3ca2'})
MERGE (a)-[r:KG_REL {predicate: '百科参考', targetWikidataId: 'WEB_10cf60906b9b3ca2'}]->(b)
ON CREATE SET r.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET r += {predicate: '百科参考', description: '北京市 的 百科来源：Beijing', weight: 0.74, source: 'WikidataSitelink', sourceUrl: 'https://en.wikipedia.org/wiki/Beijing', updatedAt: '2026-03-03T09:19:47.458345+00:00'};

MATCH (a:KgNode {wikidataId: 'Q956'})
MATCH (b:KgNode {wikidataId: 'WEB_31fba8d7846be748'})
MERGE (a)-[r:KG_REL {predicate: '百科参考', targetWikidataId: 'WEB_31fba8d7846be748'}]->(b)
ON CREATE SET r.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET r += {predicate: '百科参考', description: '北京市 的 百科来源：北京市 - 维基百科，自由的百科全书', weight: 0.6799999999999999, source: 'DuckDuckGo', sourceUrl: 'https://zh.wikipedia.org/wiki/北京市', updatedAt: '2026-03-03T09:19:47.458345+00:00'};

MATCH (a:KgNode {wikidataId: 'Q956'})
MATCH (b:KgNode {wikidataId: 'WEB_3ba6024e321400e1'})
MERGE (a)-[r:KG_REL {predicate: '百科参考', targetWikidataId: 'WEB_3ba6024e321400e1'}]->(b)
ON CREATE SET r.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET r += {predicate: '百科参考', description: '北京市 的 百科来源：北京市_百度百科', weight: 0.7, source: 'DuckDuckGo', sourceUrl: 'https://baike.baidu.com/item/北京市/65090896', updatedAt: '2026-03-03T09:19:47.458345+00:00'};

MATCH (a:KgNode {wikidataId: 'Q956'})
MATCH (b:KgNode {wikidataId: 'WEB_481368f3c8819752'})
MERGE (a)-[r:KG_REL {predicate: '百科参考', targetWikidataId: 'WEB_481368f3c8819752'}]->(b)
ON CREATE SET r.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET r += {predicate: '百科参考', description: '北京市 的 百科来源：Category:北京市旅游景点 - 维基百科，自由的百科全书', weight: 0.6799999999999999, source: 'DuckDuckGo', sourceUrl: 'https://zh.wikipedia.org/wiki/Category:北京市旅游景点', updatedAt: '2026-03-03T09:19:47.458345+00:00'};

MATCH (a:KgNode {wikidataId: 'Q956'})
MATCH (b:KgNode {wikidataId: 'WEB_89849c1a61f9103f'})
MERGE (a)-[r:KG_REL {predicate: '百科参考', targetWikidataId: 'WEB_89849c1a61f9103f'}]->(b)
ON CREATE SET r.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET r += {predicate: '百科参考', description: '北京市 的 百科来源：北京市_百度百科', weight: 0.7, source: 'DuckDuckGo', sourceUrl: 'https://baike.baidu.com/item/北京市/126069', updatedAt: '2026-03-03T09:19:47.458345+00:00'};

MATCH (a:KgNode {wikidataId: 'Q956'})
MATCH (b:KgNode {wikidataId: 'WEB_a55248a498c982bc'})
MERGE (a)-[r:KG_REL {predicate: '百科参考', targetWikidataId: 'WEB_a55248a498c982bc'}]->(b)
ON CREATE SET r.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET r += {predicate: '百科参考', description: '北京市 的 百科来源：北京市', weight: 0.74, source: 'WikidataSitelink', sourceUrl: 'https://zh.wikipedia.org/wiki/%E5%8C%97%E4%BA%AC%E5%B8%82', updatedAt: '2026-03-03T09:19:47.458345+00:00'};

MATCH (a:KgNode {wikidataId: 'Q956'})
MATCH (b:KgNode {wikidataId: 'WEB_89adbf6e9ef08182'})
MERGE (a)-[r:KG_REL {predicate: '补充参考', targetWikidataId: 'WEB_89adbf6e9ef08182'}]->(b)
ON CREATE SET r.createdAt = '2026-03-03T09:19:47.458345+00:00'
SET r += {predicate: '补充参考', description: '北京市 的 参考来源：北京市 参考页面', weight: 0.72, source: 'WikidataClaim', sourceUrl: 'https://baike.sogou.com/v4412.htm', updatedAt: '2026-03-03T09:19:47.458345+00:00'};
