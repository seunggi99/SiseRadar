import { useQuery } from '@tanstack/react-query';
import { api } from './client';

export function useMonthlyStats(lawdCd: string) {
  return useQuery({
    queryKey: ['monthly', lawdCd],
    queryFn: () => api.monthlyStats(lawdCd),
  });
}

export function useComplexRanking(lawdCd: string, ym?: string) {
  return useQuery({
    queryKey: ['complexes', lawdCd, ym ?? 'latest'],
    queryFn: () => api.complexRanking(lawdCd, ym),
  });
}
