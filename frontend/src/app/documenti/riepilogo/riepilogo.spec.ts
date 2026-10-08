import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Riepilogo } from './riepilogo';

describe('Riepilogo', () => {
  let component: Riepilogo;
  let fixture: ComponentFixture<Riepilogo>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Riepilogo],
    }).compileComponents();

    fixture = TestBed.createComponent(Riepilogo);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
