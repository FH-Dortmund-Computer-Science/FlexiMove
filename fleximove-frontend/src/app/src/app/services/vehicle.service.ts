import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { NearestAvailableVehicleResponse } from 'src/app/customer-pages/customer-homepage/customer-homepage.component';

@Injectable({ providedIn: 'root' })
export class VehicleService {
  private baseUrl = 'http://localhost:8085/vehicles';
  private results: NearestAvailableVehicleResponse[] = [];

  constructor(private http: HttpClient) {}

  getNearbyVehicles(address: string, radius?: number): Observable<NearestAvailableVehicleResponse[]> {
    let params = new HttpParams().set('address', address);
    if (radius !== undefined) {
        params = params.set('radiusInKm', radius);
    }
    return this.http.get<any[]>(`${this.baseUrl}/nearby`, { params });
  }

  getVehicleById(id: number): Observable<any> {
      return this.http.get<any[]>(`${this.baseUrl}/load/${id}`);}


  setResults(vehicles: NearestAvailableVehicleResponse[]) {
    this.results = vehicles;
  }

  getResults(): NearestAvailableVehicleResponse[] {
    return this.results;
  }

  clear() {
    this.results = [];
  }
}
