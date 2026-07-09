import { ref, computed } from 'vue';

/**
 * 导航状态管理
 * 使用全局单例模式，确保所有组件共享同一个导航状态
 */

// 全局状态
const activeSection = ref('overview');

export function useNavigation() {
  /**
   * 导航到指定的菜单项
   * @param {string} section - 菜单项的 key
   */
  function navigateTo(section) {
    activeSection.value = section;
  }

  /**
   * 获取当前激活的菜单项
   */
  const currentSection = computed(() => activeSection.value);

  return {
    currentSection,
    navigateTo,
  };
}
