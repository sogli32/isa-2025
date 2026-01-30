import { Component, OnInit, ElementRef, ViewChild, AfterViewInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BenchmarkService, TrendingBenchmarkReport, TrendingBenchmarkResult } from '../../core/services/benchmark.service';
import { GeolocationService, UserLocation } from '../../core/services/geolocation.service';

// Deklaracija za Chart.js (učitava se iz CDN)
declare var Chart: any;

@Component({
  selector: 'app-benchmark',
  templateUrl: './benchmark.html',
  styleUrls: ['./benchmark.css'],
  standalone: true,
  imports: [CommonModule, FormsModule]
})
export class BenchmarkComponent implements OnInit, AfterViewInit {

  @ViewChild('responseTimeChart') chartCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('comparisonChart') comparisonCanvas!: ElementRef<HTMLCanvasElement>;

  // Benchmark state
  report: TrendingBenchmarkReport | null = null;
  loading: boolean = false;
  error: string = '';

  // Parametri
  iterations: number = 100;
  limit: number = 20;

  // Geolokacija state
  userLocation: UserLocation | null = null;
  locationLoading: boolean = false;
  locationError: string = '';

  // Charts
  private responseTimeChart: any = null;
  private comparisonChart: any = null;

  constructor(
    private benchmarkService: BenchmarkService,
    private geolocationService: GeolocationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Automatski učitaj lokaciju pri otvaranju stranice
    this.loadUserLocation();
  }

  ngAfterViewInit(): void {
    // Chart.js se učitava dinamički
    this.loadChartJs();
  }

  /**
   * Dinamički učitava Chart.js biblioteku
   */
  private loadChartJs(): void {
    if (typeof Chart !== 'undefined') return;

    const script = document.createElement('script');
    script.src = 'https://cdn.jsdelivr.net/npm/chart.js';
    script.onload = () => console.log('Chart.js loaded');
    document.head.appendChild(script);
  }

  // ==================== GEOLOKACIJA ====================

  /**
   * Učitava lokaciju korisnika
   */
  loadUserLocation(): void {
    this.locationLoading = true;
    this.locationError = '';

    this.geolocationService.getUserLocation().subscribe({
      next: (location) => {
        this.userLocation = location;
        this.locationLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.locationError = 'Greška pri dobijanju lokacije';
        this.locationLoading = false;
        this.cdr.detectChanges();
        console.error(err);
      }
    });
  }

  /**
   * Ponovo zatraži lokaciju (briše cache)
   */
  refreshLocation(): void {
    this.geolocationService.clearCache();
    this.loadUserLocation();
  }

  /**
   * Formatira izvor lokacije za prikaz
   */
  getLocationSourceLabel(): string {
    if (!this.userLocation) return '';
    
    switch (this.userLocation.source) {
      case 'browser':
        return '📍 Precizna lokacija (Browser)';
      case 'ip_geolocation':
        return '🌐 Približna lokacija (IP adresa)';
      default:
        return '❓ Nepoznata lokacija';
    }
  }

  // ==================== BENCHMARK ====================

  /**
   * Pokreće benchmark test
   */
  runBenchmark(): void {
    this.loading = true;
    this.error = '';
    this.report = null;
    this.cdr.detectChanges();

    this.benchmarkService.runBenchmark(this.iterations, this.limit).subscribe({
      next: (report) => {
        this.report = report;
        this.loading = false;
        this.cdr.detectChanges();
        
        // Sačekaj da se DOM ažurira pa renderuj grafove
        setTimeout(() => {
          this.renderCharts();
        }, 100);
      },
      error: (err) => {
        this.error = 'Greška pri pokretanju benchmarka: ' + (err.message || 'Nepoznata greška');
        this.loading = false;
        this.cdr.detectChanges();
        console.error(err);
      }
    });
  }

  /**
   * Pokreće brzi benchmark
   */
  runQuickBenchmark(): void {
    this.loading = true;
    this.error = '';
    this.report = null;
    this.cdr.detectChanges();

    this.benchmarkService.runQuickBenchmark().subscribe({
      next: (report) => {
        this.report = report;
        this.loading = false;
        this.cdr.detectChanges();
        
        setTimeout(() => {
          this.renderCharts();
        }, 100);
      },
      error: (err) => {
        this.error = 'Greška pri pokretanju benchmarka';
        this.loading = false;
        this.cdr.detectChanges();
        console.error(err);
      }
    });
  }

  // ==================== CHARTS ====================

  /**
   * Renderuje sve grafove
   */
  private renderCharts(): void {
    if (!this.report || typeof Chart === 'undefined') return;

    this.renderResponseTimeChart();
    this.renderComparisonChart();
  }

  /**
   * Graf: Response time kroz iteracije (line chart)
   */
  private renderResponseTimeChart(): void {
    if (!this.chartCanvas || !this.report) return;

    // Uništi postojeći chart
    if (this.responseTimeChart) {
      this.responseTimeChart.destroy();
    }

    const ctx = this.chartCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    const datasets = this.report.results.map((result: TrendingBenchmarkResult, index: number) => {
      const colors = ['#e74c3c', '#2ecc71', '#3498db'];
      return {
        label: result.strategyName,
        data: result.allResponseTimes,
        borderColor: colors[index % colors.length],
        backgroundColor: colors[index % colors.length] + '20',
        tension: 0.1,
        fill: false,
        pointRadius: 1
      };
    });

    const labels = Array.from(
      { length: this.report.results[0]?.allResponseTimes?.length || 0 }, 
      (_, i) => i + 1
    );

    this.responseTimeChart = new Chart(ctx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: datasets
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          title: {
            display: true,
            text: 'Response Time kroz iteracije (ms)',
            font: { size: 16 }
          },
          legend: {
            position: 'top'
          }
        },
        scales: {
          x: {
            title: {
              display: true,
              text: 'Iteracija'
            }
          },
          y: {
            title: {
              display: true,
              text: 'Response Time (ms)'
            },
            beginAtZero: true
          }
        }
      }
    });
  }

  /**
   * Graf: Poređenje strategija (bar chart)
   */
  private renderComparisonChart(): void {
    if (!this.comparisonCanvas || !this.report) return;

    // Uništi postojeći chart
    if (this.comparisonChart) {
      this.comparisonChart.destroy();
    }

    const ctx = this.comparisonCanvas.nativeElement.getContext('2d');
    if (!ctx) return;

    const labels = this.report.results.map((r: TrendingBenchmarkResult) => r.strategyName);
    const colors = ['#e74c3c', '#2ecc71', '#3498db'];

    this.comparisonChart = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [
          {
            label: 'Avg Response Time (ms)',
            data: this.report.results.map((r: TrendingBenchmarkResult) => r.avgResponseTimeMs),
            backgroundColor: colors.map(c => c + '80'),
            borderColor: colors,
            borderWidth: 2
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          title: {
            display: true,
            text: 'Poređenje prosečnog vremena odziva',
            font: { size: 16 }
          },
          legend: {
            display: false
          }
        },
        scales: {
          y: {
            title: {
              display: true,
              text: 'Response Time (ms)'
            },
            beginAtZero: true
          }
        }
      }
    });
  }

  // ==================== HELPERS ====================

  /**
   * Formatira timestamp
   */
  formatTimestamp(timestamp: string): string {
    return new Date(timestamp).toLocaleString('sr-RS');
  }

  /**
   * Vraća CSS klasu za strategiju (za highlighting optimalne)
   */
  getStrategyClass(strategyName: string): string {
    if (this.report && strategyName === this.report.optimalStrategy) {
      return 'optimal';
    }
    return '';
  }

  /**
   * Računa speedup faktor u odnosu na REAL_TIME
   */
  getSpeedup(result: TrendingBenchmarkResult): string {
    if (!this.report) return '';
    
    const realTime = this.report.results.find((r: TrendingBenchmarkResult) => r.strategyName === 'REAL_TIME');
    if (!realTime || result.strategyName === 'REAL_TIME') return '';
    
    const speedup = realTime.avgResponseTimeMs / result.avgResponseTimeMs;
    return `${speedup.toFixed(1)}x brže`;
  }
}