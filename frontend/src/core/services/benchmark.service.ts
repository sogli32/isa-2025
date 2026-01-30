import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * Rezultat jedne benchmark strategije
 */
export interface TrendingBenchmarkResult {
  strategyName: string;
  description: string;
  iterations: number;
  avgResponseTimeMs: number;
  minResponseTimeMs: number;
  maxResponseTimeMs: number;
  medianResponseTimeMs: number;
  p95ResponseTimeMs: number;
  p99ResponseTimeMs: number;
  standardDeviation: number;
  isRealTime: boolean;
  videoCount: number;
  allResponseTimes: number[];
}

/**
 * Kompletan benchmark izveštaj
 */
export interface TrendingBenchmarkReport {
  timestamp: string;
  totalVideosInDatabase: number;
  iterationsPerStrategy: number;
  results: TrendingBenchmarkResult[];
  recommendation: string;
  optimalStrategy: string;
}

/**
 * Servis za benchmark testiranje trending strategija.
 * 
 * [S2] Potrebno je pronaći i dokazati optimalnu meru između performansi 
 * i trendinga u realnom vremenu.
 */
@Injectable({
  providedIn: 'root'
})
export class BenchmarkService {

  private apiUrl = 'http://localhost:8080/api/benchmark';

  constructor(private http: HttpClient) {}

  /**
   * Pokreće kompletan benchmark test svih trending strategija.
   * 
   * @param iterations Broj iteracija za svaku strategiju (default: 100)
   * @param limit Broj videa za vraćanje u trending listi (default: 20)
   */
  runBenchmark(iterations: number = 100, limit: number = 20): Observable<TrendingBenchmarkReport> {
    return this.http.get<TrendingBenchmarkReport>(
      `${this.apiUrl}/trending?iterations=${iterations}&limit=${limit}`
    );
  }

  /**
   * Brzi benchmark sa manjim brojem iteracija.
   */
  runQuickBenchmark(): Observable<TrendingBenchmarkReport> {
    return this.http.get<TrendingBenchmarkReport>(`${this.apiUrl}/trending/quick`);
  }
}
