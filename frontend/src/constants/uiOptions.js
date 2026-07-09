export const STORAGE_KEYS = {
  token: "tourismqa_token",
  user: "tourismqa_user",
  provider: "tourismqa_provider",
  model: "tourismqa_model",
  exploreDraft: "tourismqa_explore_draft",
};

export const budgetOptions = ["经济型", "舒适型", "品质型"];

export const memoryOptions = [
  { value: "STANDARD", label: "标准记忆" },
  { value: "RECENT_ONLY", label: "仅保留近期消息" },
  { value: "PRIVACY_FIRST", label: "隐私优先" },
];

export const starterPrompts = [
  {
    title: "周末城市漫游",
    description: "例如：帮我安排上海周末两日游",
    prompt: "帮我安排一个适合年轻人的上海周末两日游，重点放在城市漫步、咖啡店和夜景。",
    icon: "sparkles",
  },
  {
    title: "假期出行规划",
    description: "例如：国庆去成都怎么玩",
    prompt: "国庆假期去成都玩 4 天，预算中等，想兼顾美食和轻松观光，怎么安排？",
    icon: "map",
  },
  {
    title: "预算型路线建议",
    description: "例如：学生党三日游怎么省钱",
    prompt: "学生党去杭州玩 3 天，预算有限，帮我做一份省钱但体验不错的行程建议。",
    icon: "wallet",
  },
];
